#!/usr/bin/perl

use strict;
use File::Basename;
use POSIX qw(strftime);
use Getopt::Long;
use Data::Dumper;
use GD;
use File::Find;

my $boardSize = 512;
my $logFolder = 'logs';
my $resultsFolder = 'results';
my $htmlPlaceholder = '<!--__ITEM__-->';
my $scriptName = $0;
if ($scriptName=~/^(.+)\/([^\/]+)$/o) {
	$scriptName = $2;
	chdir $1;
}
usage("No '$logFolder' exists, please create one") unless (-d $logFolder);
usage("No '$resultsFolder' exists, please create one") unless (-d $resultsFolder);

sub usage {
	print @_, "\n";
	print <<EOM;
Usage: perl $scriptName [options]
Options:
   -c --compile      Compile the game
   -r --run N        Run the game N times
   -a --archive      Archive generated log files (applicable only with -r)
   -s --save FILE    Save analysis results in FILE (under '$resultsFolder' subfolder)
   -f --folder PATH  Analyze given folder (instead of '$logFolder')
      --nolog        Comment all logging in program (to make it faster)
      --log          Turn on all logging in program
   -h -? --help      This help message
EOM
	exit 1;
}

my $board = {};
my $clcompile = 0;
my $clrun = 0;
my $clsave = undef;
my $clfolder = undef;
my $clnolog = 0;
my $cllog = 0;
my $clhelp = 0;
Getopt::Long::Configure ("bundling");
GetOptions(
	'c|compile'=>\$clcompile,
	'r|run=i'=>\$clrun,
	's|save:s'=>\$clsave,
	'f|folder=s'=>\$clfolder,
	'nolog'=>\$clnolog, 'log'=>\$cllog,
	'h|?|help'=>\$clhelp
) or usage();
usage() if ($clhelp);
$clcompile = 1 if ($clcompile);
#print "c=[$clcompile] r=[$clrun]\n"; exit;
usage("-f can't be specified with -r") if (defined $clfolder and $clrun);
usage("--nolog must be specified alone when specified") if ($clnolog and ($cllog || $clcompile || $clrun || defined $clsave || defined $clfolder || $clhelp));
usage("--log must be specified alone when specified") if ($cllog and ($clnolog || $clcompile || $clrun || defined $clsave || defined $clfolder || $clhelp));

if ($clnolog || $cllog) {
	modify_source_code();
	exit;
}

if ($clrun) {
	compile_project() if ($clcompile);
	my $n = $clrun;
	while ($n--) {
		logm("--------------------------------\n") if ($clrun > 1);
		my $gameResult = run_game($n);
		my $folder = $logFolder;
		$folder = archive_logs();
		my $res = analyze_folder($folder);
		$res->{gameruntime} = $gameResult->{gameruntime};
		$res->{player} = $gameResult->{player};
		$res->{rounds} = $gameResult->{rounds};
		record_analysis($res);
	}
} else {
	my $res = analyze_folder($clfolder || $logFolder);
	record_analysis($res);
}

sub modify_source_code {
	find({ wanted => \&process_file, no_chdir => 1 }, 'ants-zoran/src');
}

sub process_file {
	my $fpath = $_;
	return if (-d $fpath);
	return unless ($fpath=~m/\.java$/o);
	my $contents = '';
	open(my $fh, "<$fpath") or fail("Can't read $fpath");
	while (my $line = <$fh>) {
		if ($clnolog) {
			if ($line=~m/^[^\/].*Logger\./o) {
				$contents .= "//L$line";
			} else {
				$contents .= $line;
			}
		} else {
			if ($line=~m/^\/\/L(.*)$/o) {
				$contents .= "$1\n";
			} else {
				$contents .= $line;
			}
		}
	}
	close($fh);
	open($fh, ">$fpath") or fail("Can't write $fpath");
	print $fh $contents;
	close($fh);
}

sub record_analysis {
	my ($res) = @_;
	if ($clrun || defined $clsave) {
		# Save summary in archive.txt or adhoc.txt
		my $fname = $clsave;
		$fname = $res->{folder} unless (length($fname));
		if ($fname eq $logFolder) {
			$fname = 'adhoc';
		} else {
			$fname = basename($fname);
		}
		$fname = "$resultsFolder/$fname.txt";
		logm("Saving overview to $fname ... ");
		open (my $fh, ">>$fname");
		print $fh $res->{summary};
		print $fh "--------\n";
		close($fh);
		logm("done\n");
		# runs.txt
		$fname = "$resultsFolder/runs.txt";
		logm("Recording results to $fname\n");
		if (-f $fname) {
			open ($fh, ">>$fname") or fail("Can't write results to '$fname'");
		} else {
			open ($fh, ">$fname") or fail("Can't write results to '$fname'");
			print $fh "date\t\tmissing\tfill%\tcells\trounds\tfood\tants\tgame time\tturn time\tfolder\t\t\t\tfail\n";
		}
		print $fh "$res->{date}\t$res->{missing}\t$res->{cells}->{percent}\t$res->{cells}->{n}";
		print $fh "\t$res->{rounds}\t$res->{player}->{1}->{food}\t$res->{player}->{1}->{ants}";
		print $fh "\t$res->{gameruntime}\t$res->{avgturntime}\t$res->{fail}\n";
		close($fh);
		# run.html
		$fname = "$resultsFolder/runs.html";
		logm("Recording results to $fname\n");
		my $html = '';
		if (-f $fname) {
			open ($fh, "<$fname") or fail("Can't read results html '$fname'");
			local $/ = undef;
			$html = <$fh>;
			close($fh);
		} else {
			$html = new_run_html();
		}
		# full board html
		my $fullBoardFile = "full_board_$res->{time}";
		my $newRow = "<tr>";
		$newRow .= new_td($res->{date},undef);
		$newRow .= new_td($res->{missing},undef);
		$newRow .= new_td($res->{cells}->{percent}.'%',undef);
		$newRow .= new_td(new_href($res->{cells}->{n},"file:$fullBoardFile.png"),undef);
		$newRow .= new_td($res->{rounds}, undef);
		$newRow .= new_td($res->{player}->{1}->{food}, undef);
		$newRow .= new_td($res->{player}->{1}->{ants}, undef);
		$newRow .= new_td($res->{gameruntime},undef);
		$newRow .= new_td($res->{avgturntime},undef);
		$newRow .= new_td($res->{fail},'red');
		$newRow .= "</tr>\n";
		$html=~s/$htmlPlaceholder/$newRow  $htmlPlaceholder/go;
		open ($fh, ">$fname") or fail("Can't write results html '$fname'");
		print $fh $html;
		close($fh);
		create_png($res,"$resultsFolder/$fullBoardFile.png");
	} else {
		print $res->{summary};
		print "--------\n" if ($clrun);
	}
}

sub new_run_html {
	return <<EOT;
<html><head><title>Run results</title></head>
<body><table border="1" cellspacing="0" cellpadding="3">
  <tr>
    <th>Date</th><th>Missing</th><th>Fill%</th><th>Cells</th>
    <th>Rounds</th><th>Food</th><th>Ants</th>
    <th>Run time</th><th>Turn time</th><th>Note</th></tr>
  $htmlPlaceholder
</table>
</body></html>
EOT
}

sub create_png {
	my ($stat,$fname) = @_;
	GD::Image->trueColor(1);
	my $cellSizeX = 6;
	my $cellSizeY = 4;
	my $im = new GD::Image($boardSize * $cellSizeX, $boardSize * $cellSizeY);
	my $white = $im->colorAllocate(255, 255, 255);
	my $lightYellow = $im->colorAllocate(255, 245, 220);
	my $red = $im->colorAllocate(255, 0, 0);
	my $darkBlue = $im->colorAllocate(0, 0, 180);
	my $black = $im->colorAllocate(0, 0, 0);
	my $emptyBg = $im->colorAllocate(204, 222, 216);
	# make the background transparent and interlaced
#	$im->transparent($white);
	$im->interlaced('true');
#	$im->alphaBlending(0);
#	$im->saveAlpha(1);
	$im->fill(2,2,$emptyBg);
	my $x0 = $stat->{board}->{bx0};
	my $x1 = $stat->{board}->{bx1};
	my $y0 = $stat->{board}->{by0};
	my $y1 = $stat->{board}->{by1};
	my ($xnest,$ynest);
	my $color;
	for (my $y = $y0; $y <= $y1; $y++) {
		for (my $x = $x0; $x <= $x1; $x++) {
			my $px = $x - $x0;
			my $py = $y - $y0;
			my $c = ' ';
			$c = $stat->{board}->{board}->{$x}->{$y}->{v} if (defined $stat->{board}->{board}->{$x}->{$y});
			if ($c eq ' ') { $color = $white; }
			elsif ($c eq '.') { $color = $lightYellow; }
			elsif ($c eq '#') { $color = $black; }
			elsif ($c eq 'N') { $color = $red; $xnest = $px; $ynest = $py; }
			else { fail("check PNG color for '$c'") }
			$im->filledRectangle($cellSizeX * $px, $cellSizeY * $py, $cellSizeX * ($px+1), $cellSizeY * ($py+1), $color);
		}
	}
	my $title = "$stat->{cells}->{n} cells [$stat->{cells}->{percent}%% fill]  board: $stat->{board}->{bsizeX} x $stat->{board}->{bsizeY} - $stat->{date}";
	draw_title($im,4,2,$lightYellow,$red,gdMediumBoldFont,$title);
	$im->filledRectangle($cellSizeX * ($xnest-1), $cellSizeY * ($ynest-1), $cellSizeX * ($xnest+2), $cellSizeY * ($ynest+2), $red);
	open (my $fh, ">$fname") or fail("Can't write to '$fname'");
	binmode $fh;
	print $fh $im->png;
	close($fh);
}

sub draw_title {
	my ($im,$x,$y,$bg,$fg,$font,$title) = @_;
	my $font = gdLargeFont;
	$im->string($font, $x-2, $y-2, $title, $bg);
	$im->string($font, $x-2, $y+2, $title, $bg);
	$im->string($font, $x+2, $y+2, $title, $bg);
	$im->string($font, $x+2, $y-2, $title, $bg);
	$im->string($font, $x-1, $y-1, $title, $bg);
	$im->string($font, $x-1, $y+1, $title, $bg);
	$im->string($font, $x+1, $y+1, $title, $bg);
	$im->string($font, $x+1, $y-1, $title, $bg);
	$im->string($font, $x, $y, $title, $fg);
}

sub represented_html_game_cell {
	my ($b,$x,$y) = @_;
	return ' ' unless (defined $b->{$x}->{$y});
	my $c = $b->{$x}->{$y}->{v};
	my $color;
	if ($c eq '#') { $color = '#000000'; }
	elsif ($c eq 'N') { $color = '#FF0000'; }
	elsif ($c eq '.') { $color = '#F7E477'; }
	else { fail("check character '$c' in board rep"); }
	return "<span style=\"color: $color;\">&#x25A0;</span>";
}

sub new_href {
	my ($text,$url) = @_;
	my $str = "";
	$str .= "<a href=\"$url\">";
	$str .= $text;
	$str .= "</a>";
	return $str;
}

sub new_td {
	my ($text,$color) = @_;
	my $str = "<td";
	$str .= " style=\"color: $color;\"" if (defined $color);
	$str .= '>';
	$str .= $text;
	$str .= "</td>";
	return $str;
}

sub analyze_folder {
	my ($folder) = @_;
	logm("Analyzing folder '$folder'\n");
	fail("No logs folder generated in '$folder'") unless (-d $folder);
	my $t = 0;
	for (my $i = 2; $i <= 33; $i++) {
		my $num = $i;
		$num = "0$num" unless (length($num)>1);
		$board->{scout}->{$num} = read_board($folder,$num);
		$t = $board->{scout}->{$num}->{time} if ($t < $board->{scout}->{$num}->{time});
		unless (defined $board->{scout}->{$num}->{board}) {
			$board->{missing}->{count}++;
			$board->{missing}->{names} .= "$num,";
		}
	}
	logm("Joining ", scalar keys %{$board->{scout}}," boards\n");
	my $computed = {name=>'computed', board=>{}, time=>$t};
	foreach my $num (sort keys %{$board->{scout}}) {
		join_board($computed, $board->{scout}->{$num});
	}
	logm("Analyzing joined board\n");
	foreach my $x (keys %{$computed->{board}}) {
		foreach my $y (keys %{$computed->{board}->{$x}}) {
			my $c = $computed->{board}->{$x}->{$y}->{v};
			$computed->{state}->{cells}++;
			if ($c eq '.') { $computed->{state}->{empty}++; }
			elsif ($c eq 'N') { $computed->{state}->{empty}++, $computed->{state}->{nest}++; }
			elsif ($c eq '#') { $computed->{state}->{obstacle}++; }
			else { fail("Add support for cell type '$c'"); }
		}
	}
	$computed->{state}->{max} = ($boardSize - 2) * ($boardSize - 1);
	$computed->{state}->{unknown} = $computed->{state}->{max} - $computed->{state}->{cells};
	my $res = {folder=>$folder, missing=>0, summary=>''};
	$res->{board} = $computed;
	$res->{fail} .= "Missing nest\n" unless ($computed->{state}->{nest} == 1);
	$res->{time} = $computed->{time};
	$res->{date} = strftime("%Y-%m-%d %H:%M:%S",localtime($computed->{time}));
	$res->{summary} .= "$res->{date} $res->{folder}\n";
	$res->{summary} .= $res->{fail} if (defined $res->{fail});
	if (defined $board->{missing}->{count}) {
		$res->{missing} = $board->{missing}->{count};
		$res->{summary} .= "$board->{missing}->{count} missing ants\n" ;
	}
	add_stat($res,$computed,'cells','max');
	add_stat($res,$computed,'unknown','max');
	add_stat($res,$computed,'empty','cells');
	add_stat($res,$computed,'obstacle','cells');
	$res->{summary} .= "all good, times: [".join(' ',times)."]\n";
	$res->{avgturntime} = 0;
	if (open (my $fh, "<$folder/info.txt")) {
		while (my $line = <$fh>) {
			if ($line=~m/Average run-time: ([0-9]+)$/o) {
				my $t = $1;
				$res->{avgturntime} = $t if ($res->{avgturntime} < $t);
			}
		}
		close($fh);
	}
	return $res;
}

sub add_stat {
	my ($res,$computed,$key,$totKey) = @_;
	my $n = $computed->{state}->{$key};
	my $tot = $computed->{state}->{$totKey};
	my $p = '0';
	$p = sprintf("%.2g", $n/$tot*100) if ($tot > 0);
	$res->{$key}->{n} = $n;
	$res->{$key}->{percent} = $p;
	$res->{summary} .= "$key: $n / $tot [$p%]\n";
}

sub archive_logs {
	my $archiveFolder = "$logFolder/archive_".time();
	mkdir $archiveFolder;
	run_command("mv $logFolder/*.txt $archiveFolder/");
	return $archiveFolder;
}

sub compile_project {
	my $cmdCompile = 'gradle build';
	run_command($cmdCompile);
#	my $cmdCompile = 'javac -d build -cp lib/ants-api.jar:lib/ants-server.jar ants-zoran/src/main/java/org/linkedin/contest/ants/zoran/*.java';
#	my $cmdBuild = 'jar cf lib/ants-zoran.jar build/org/';
#	run_command($cmdCompile);
#	run_command($cmdBuild);
}

sub run_game {
	my ($gameNumber) = @_;
	my $gameResult = {};
	my @t0 = times();
	my $cmdRun = 'java -cp lib/ants-api.jar:lib/ants-server.jar:ants-zoran/build/libs/ants-zoran.jar';
#	my $cmdRun = 'java -ea -cp lib/ants-api.jar:lib/ants-server.jar:lib/ants-zoran.jar';
	$cmdRun .= ' org/linkedin/contest/ants/server/AntServer';
	$cmdRun .= ' -B -p1 org.linkedin.contest.ants.zoran.ZoranAnt -p2 org.linkedin.contest.ants.zoran.DoNothingAnt';
	logm("Running game $cmdRun ... ");
	my $s = `$cmdRun`;
	if ($?) {
		fail("Game crashed:\n\nexit code $?\n$!");
	}
	logm("done\n");
	my @t1 = times();
	$gameResult->{gameruntime} = actual_times(\@t0,\@t1);
	logm("Run time: $gameResult->{gameruntime}\n");
	my $player = 0;
	foreach my $line (split(/\n/,$s)) {
		if ($line=~m/^Player ([0-9]):/o) {
			$player = $1;
		} elsif ($line=~m/^\s+Food: ([0-9]+)/o) {
			my $n = $1;
			fail("Check results parsing, no player for food count $n") unless ($player > 0);
			$gameResult->{player}->{$player}->{food} = $n;
		} elsif ($line=~m/^\s+Ants: ([0-9]+)/o) {
			my $n = $1;
			fail("Check results parsing, no player for ants count $n") unless ($player > 0);
			$gameResult->{player}->{$player}->{ants} = $n;
		} elsif ($line=~m/^Rounds played: ([0-9]+)/o) {
			my $n = $1;
			$gameResult->{rounds} = $n;
		}
	}
	if (open (my $fh, ">$logFolder/output_$gameNumber.txt")) {
		print $fh $s;
		close($fh);
	}
	return $gameResult;
}

sub actual_times {
	my ($t0,$t1) = @_;
	return $t1->[0] - $t0->[0] + $t1->[1] - $t0->[1] + $t1->[2] - $t0->[2] + $t1->[3] - $t0->[3];
}

sub logm {
	print STDERR @_;
}

sub join_board {
	my ($b1,$b2) = @_;
	return unless (defined $b2->{bx0});
	return unless (defined $b2->{board});
	set_value($b1,$b2,'bx0',-1);
	set_value($b1,$b2,'bx1',1);
	set_value($b1,$b2,'by0',-1);
	set_value($b1,$b2,'by1',1);
	set_value($b1,$b2,'turn',1);
	set_value($b1,$b2,'bsizeX',1);
	set_value($b1,$b2,'bsizeY',1);
	foreach my $x (keys %{$b2->{board}}) {
		foreach my $y (keys %{$b2->{board}->{$x}}) {
			my $c = $b2->{board}->{$x}->{$y}->{v};
			set_cell($b1,$x,$y,$c,$b2->{name}) if ($c ne ' ');
		}
	}
}

sub set_value {
	my ($b1,$b2,$key,$direction) = @_;
	fail("No key '$key' in $b2->{name}") unless (defined $b2->{$key});
	if (!defined $b1->{$key}) {
		$b1->{$key} = $b2->{$key};
	} elsif ($direction == 0) {
		$b1->{$key} = $b2->{$key} if ($b1->{$key} ne $b2->{$key});
	} elsif ($direction == 1) {
		$b1->{$key} = $b2->{$key} if ($b1->{$key} < $b2->{$key});
	} elsif ($direction == -1) {
		$b1->{$key} = $b2->{$key} if ($b1->{$key} > $b2->{$key});
	}
}

sub run_command {
	my ($cmd) = @_;
	logm("Running: $cmd ... ");
	my $s = `$cmd`;
	if ($?) {
		fail("Can't run command $cmd:\n\nexit code $?\n$!");
	}
	logm("done\n");
}

sub compare_boards {
	my ($b1,$b2) = @_;
	foreach my $x (keys %{$b1->{board}}) {
		foreach my $y (keys %{$b1->{board}->{$x}}) {
			my $c1 = $b1->{board}->{$x}->{$y}->{v};
			my $c2 = ' ';
			$c2 = $b2->{board}->{$x}->{$y}->{v} if (defined $b2->{board}->{$x}->{$y}->{v});
			fail("Board mismatch $b2->{name} vs $b1->{name}: $c2 vs $c1") unless ($c1 eq $c2);
		}
	}
}

sub set_cell {
	my ($b,$x,$y,$char,$comingFrom) = @_;
	$char = '.' if ($char eq '%');
	$char = '.' if ($char eq '^');
	my $old = '';
	my $prev = '';
	if (defined defined $b->{board}->{$x}->{$y}) {
		$old = $b->{board}->{$x}->{$y}->{v};
		$prev = $b->{board}->{$x}->{$y}->{from};
	}
	fail("Bad value for $b->{name}, x=$x y=$y c='$char' old='$old', prev='$prev', new='$comingFrom'") unless (can_replace_cell($old,$char));
	$b->{board}->{$x}->{$y}->{v} = $char;
	$b->{board}->{$x}->{$y}->{from} = "$prev.$comingFrom";
}

sub can_replace_cell {
	my ($old,$new) = @_;
	return 1 unless (defined $old);
	return 1 if ($old eq '');
	return 1 if ($old eq ' ');
	return 1 if ($old eq $new);
	return 0;
}

sub read_board {
	my ($folder,$num) = @_;
	my $res = {name=>$num, time=>0};
	my $fname = "$folder/board_${num}.txt";
	return $res unless (-f $fname);
	$res->{name} = substr($fname,length("$folder/board_"));
	$res->{name}=~s/\.txt$//o;
	$res->{name}=~s/_/ /go;
	$res->{name}=~s/000$/K/go;
	my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,$atime,$mtime,$ctime,$blksize,$blocks) = stat($fname);
	$res->{time} = $mtime;
	open(my $fh, "<$fname") or fail("Can't read file $fname");
	my $line = <$fh>;
	if ($line=~m/^([0-9]+) ([0-9]+) xy=\[([0-9]+),([0-9]+)\] bs=\[([0-9]+),([0-9]+),([0-9]+)\]/o) {
		$res->{turn} = $1;
		$res->{id} = $2;
		$res->{posX} = $3;
		$res->{posY} = $4;
		$res->{bsizeX} = $5;
		$res->{bsizeY} = $6;
		$res->{known} = $7;
	} else {
		fail("Malformed line 1 in $fname: $line");
	}
	$line = <$fh>;
	if ($line=~m/^Board x=([0-9]+)-([0-9]+) y=([0-9]+)-([0-9]+) known=([0-9]+)$/o) {
		$res->{bx0} = $1;
		$res->{bx1} = $2;
		$res->{by0} = $3;
		$res->{by1} = $4;
		$res->{known} = $5;
	} else {
		fail("Malformed line 2 in $fname: $line");
	}
	$line = <$fh>;
	my $n = 3;
	my $size = 0;
#10003 2 xy=[631,480] bs=[271,108] f=[91,203] Scout section 0 returning
#Board x=511-782 y=471-579
#         5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |    5    |
	$res->{board} = {};
	while ($line = <$fh>) {
		$n++;
		if ($line=~m/^ *([0-9]+) ([ \.N#%^]+)$/o) {
			my $y = $1;
			my $rep = $2;
			if ($size == 0) {
				$size = length($rep);
			} elsif ($size != length($rep)) {
				fail("Malformed line $n in $fname");
			}
			for (my $i = 0; $i<length($rep); $i++) {
				set_cell($res,$res->{bx0}+$i,$y,substr($rep,$i,1),$res->{name});
			}
		} else {
			fail("Malformed line $n in $fname: $line");
		}
	}
	close($fh);
	return $res;
}

sub fail {
	print "@_\n";
	exit 1;
}
