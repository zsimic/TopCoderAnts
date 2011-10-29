#!/usr/bin/perl

use strict;
use File::Basename;
use POSIX qw(strftime);
use Getopt::Long;
use Data::Dumper;
use GD;

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
   -h -? --help      This help message
EOM
	exit 1;
}

my $board = {};
my $clcompile = 0;
my $clrun = 0;
my $clsave = undef;
my $clfolder = undef;
my $clhelp = 0;
Getopt::Long::Configure ("bundling");
GetOptions(
	'c|compile'=>\$clcompile,
	'r|run=i'=>\$clrun,
	's|save:s'=>\$clsave,
	'f|folder=s'=>\$clfolder,
	'h|?|help'=>\$clhelp
) or usage();
usage() if ($clhelp);
$clcompile = 1 if ($clcompile);
#print "c=[$clcompile] r=[$clrun]\n"; exit;
usage("-f can't be specified with -r") if (defined $clfolder and $clrun);

if ($clrun) {
	compile_project() if ($clcompile);
	my $n = $clrun;
	while ($n--) {
		my $elapsed = run_game();
		my $folder = $logFolder;
		$folder = archive_logs();
		my $res = analyze_folder($folder);
		$res->{gameruntime} = $elapsed;
		record_analysis($res);
	}
} else {
	my $res = analyze_folder($clfolder || $logFolder);
	record_analysis($res);
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
			print $fh "date\t\tmissing\tfill%\tcells\tgameruntime\tfolder\t\t\t\tfail\n";
		}
		print $fh "$res->{date}\t$res->{missing}\t$res->{cells}->{percent}\t$res->{cells}->{n}\t$res->{gameruntime}\t$res->{fail}\n";
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
		$newRow .= new_td($res->{gameruntime},undef);
		$newRow .= new_td($res->{folder},undef);
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

sub create_png {
	my ($stat,$fname) = @_;
	GD::Image->trueColor(1);
	my $cellSizeX = 3;
	my $cellSizeY = 2;
	my $im = new GD::Image($boardSize * $cellSizeX, $boardSize * $cellSizeY);
	my $white = $im->colorAllocate(255, 255, 255);
	my $lightYellow = $im->colorAllocate(255, 245, 220);
	my $lightGreen = $im->colorAllocate(150, 255, 150);
	my $red = $im->colorAllocate(255, 0, 0);
	my $darkBlue = $im->colorAllocate(0, 0, 180);
	my $green = $im->colorAllocate(0, 255, 0);
	my $black = $im->colorAllocate(0, 0, 0);
	# make the background transparent and interlaced
	$im->transparent($white);
	$im->interlaced('true');
	$im->alphaBlending(0);
	$im->saveAlpha(1);
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
			elsif ($c eq '%') { $color = $lightGreen; }
			elsif ($c eq '^') { $color = $green; }
			elsif ($c eq 'N') { $color = $red; $xnest = $px; $ynest = $py; }
			else { fail("check PNG color for '$c'") }
			$im->filledRectangle($cellSizeX * $px, $cellSizeY * $py, $cellSizeX * ($px+1), $cellSizeY * ($py+1), $color);
		}
	}
	$im->stringFT($darkBlue, "a-ttf-font.ttf", 12, 0, 1000, 20, "$stat->{cells}->{percent}%% $stat->{date}");
	$im->filledRectangle($cellSizeX * ($xnest-1), $cellSizeY * ($ynest-1), $cellSizeX * ($xnest+2), $cellSizeY * ($ynest+2), $red);
	open (my $fh, ">$fname") or fail("Can't write to '$fname'");
	binmode $fh;
	print $fh $im->png;
	close($fh);
}

sub represented_html_game_cell {
	my ($b,$x,$y) = @_;
	return ' ' unless (defined $b->{$x}->{$y});
	my $c = $b->{$x}->{$y}->{v};
	my $color;
	if ($c eq '#') { $color = '#000000'; }
	elsif ($c eq 'N') { $color = '#FF0000'; }
	elsif (($c eq '%') or ($c eq '^')) { $color = '#00FF00'; }
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

sub new_run_html {
	return <<EOT;
<html><head><title>Run results</title></head><body>
<table>
  <tr><th>Date</th><th align="center">Missing</th><th>Fill%</th><th>Cells</th><th>Run time</th><th>Note</th></tr>
  $htmlPlaceholder
</table>
</body></html>
EOT
}

sub analyze_folder {
	my ($folder) = @_;
	logm("Analyzing folder '$folder'\n");
	fail("No logs folder generated in '$folder'") unless (-d $folder);
	my $t = 0;
	for (my $i = 2; $i <= 33; $i++) {
		my $num = $i;
		$num = "0$num" unless (length($num)>1);
		$board->{scout}->{$num} = read_board($folder,"board_${num}_done.txt");
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
			elsif ($c eq '%' || $c eq '^') { $computed->{state}->{empty}++, $computed->{state}->{food}++, ; }
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
	add_stat($res,$computed,'food','empty');
	$res->{summary} .= "all good, times: [".join(' ',times)."]\n";
	return $res;
}

sub add_stat {
	my ($res,$computed,$key,$totKey) = @_;
	my $n = $computed->{state}->{$key};
	my $tot = $computed->{state}->{$totKey};
	my $p = sprintf("%.2g", $n/$tot*100);
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
	my @t0 = times();
	my $cmdRun = 'java -ea -cp lib/ants-api.jar:lib/ants-server.jar:ants-zoran/build/libs/ants-zoran.jar';
#	my $cmdRun = 'java -ea -cp lib/ants-api.jar:lib/ants-server.jar:lib/ants-zoran.jar';
	$cmdRun .= ' org/linkedin/contest/ants/server/AntServer';
	$cmdRun .= ' -B -p1 org.linkedin.contest.ants.zoran.ZoranAnt -p2 org.linkedin.contest.ants.zoran.DoNothingAnt';
	run_command($cmdRun);
	my @t1 = times();
	my $elapsed = actual_times(\@t0,\@t1);
	logm("Run time: $elapsed\n");
	return $elapsed;
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
	my ($folder,$name) = @_;
	my $res = {name=>$name, time=>0};
	my $fname = "$folder/$name";
	return $res unless (-f $fname);
	my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,$atime,$mtime,$ctime,$blksize,$blocks) = stat($fname);
	$res->{time} = $mtime;
	open(my $fh, "<$fname") or fail("Can't read file $fname");
	my $line = <$fh>;
	if ($line=~m/^([0-9]+) ([0-9]+) xy=\[([0-9]+),([0-9]+)\] bs=\[([0-9]+),([0-9]+),([0-9]+)\] f=\[([0-9]+),([0-9]+)\]/o) {
		$res->{turn} = $1;
		$res->{id} = $2;
		$res->{posX} = $3;
		$res->{posY} = $4;
		$res->{bsizeX} = $5;
		$res->{bsizeY} = $6;
		$res->{known} = $7;
		$res->{foodCells} = $8;
		$res->{foodTotal} = $9;
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
				set_cell($res,$res->{bx0}+$i,$y,substr($rep,$i,1),$name);
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
