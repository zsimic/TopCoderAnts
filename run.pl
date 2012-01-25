#!/usr/bin/perl

use strict;
use File::Basename;
use POSIX qw(strftime);
use Getopt::Long;
use Data::Dumper;
use File::Find;
use Math::Complex;
use Math::Trig;

my $boardSize = 512;
my $logFolder = 'logs';
my $resultsFolder = "$logFolder/results";
my $baseRunsFolder = "$resultsFolder/runs";
my $htmlPlaceholder = '<!--__ITEM__-->';
my $scriptName = $0;
if ($scriptName=~/^(.+)\/([^\/]+)$/o) {
	$scriptName = $2;
	chdir $1;
}

sub usage {
	print @_, "\n";
	print <<EOM;
Usage: perl $scriptName [options]
Options:
   -c --compile      Compile the game
   -r --run N        Run the game N times
      --debug        Run server in debug mode
      --seed         Specify seed to pass to server
   -a --archive      Archive generated log files (applicable only with -r)
   -s --save FILE    Save analysis results in FILE (under '$resultsFolder' subfolder)
      --nolog        Comment all logging in program (to make it faster)
      --log          Turn on all logging in program
      --dry          Dry run for --nolog or --log
   -h -? --help      This help message
EOM
	exit 1;
}

my $board = {};
my $clcompile = 0;
my $clrun = 0;
my $cldebug = 0;
my $clseed = 0;
my $clarchive = 0;
my $clsave = undef;
my $clnolog = 0;
my $cllog = 0;
my $cldry = 0;
my $clhelp = 0;
Getopt::Long::Configure ("bundling");
GetOptions(
	'c|compile'=>\$clcompile,
	'r|run=i'=>\$clrun, 'debug'=>\$cldebug, 'seed:i'=>\$clseed, 'archive'=>\$clarchive,
	's|save:s'=>\$clsave,
	'nolog'=>\$clnolog, 'log'=>\$cllog, 'dry'=>\$cldry,
	'h|?|help'=>\$clhelp
) or usage();
usage() if ($clhelp);
$clcompile = 1 if ($clcompile);
#print "c=[$clcompile] r=[$clrun]\n"; exit;
usage("--nolog must be specified alone when specified") if ($clnolog and ($cllog || $clcompile || $clrun || defined $clsave || $clhelp));
usage("--log must be specified alone when specified") if ($cllog and ($clnolog || $clcompile || $clrun || defined $clsave || $clhelp));

if ($clnolog || $cllog) {
	modify_source_code();
	exit;
}


if ($clrun || defined $clsave) {
	mkdir($logFolder) unless (-d $logFolder);
	mkdir($resultsFolder) unless (-d $resultsFolder);
	mkdir($baseRunsFolder) unless (-d $baseRunsFolder);
}
if ($clrun) {
	if ($clcompile) {
		if ($cldebug) {
			$clnolog = 0;
			$cllog = 1;
		} else {
			$clnolog = 1;
			$cllog = 0;
		}
		modify_source_code();
		compile_project();
		if ($cldebug) {
			$clnolog = 1;
			$cllog = 0;
			modify_source_code();
		}
	}
	my $n = $clrun;
	my $gameId = time;
	my $dateRep = strftime("%Y-%m-%d %H:%M:%S",localtime($gameId));
	add_html_line("$resultsFolder/index.html",new_main_index_html(),"<li><a href=\"runs/$gameId.html\">$dateRep</a>\n");
	while ($n--) {
		my $gameResult = run_game($n,$gameId,$n,$baseRunsFolder);
		archive_logs($gameResult) if ($cldebug && $clarchive);
		analyze_folder($gameResult);
		record_analysis($gameResult);
	}
} else {
	my $gameResult = {};
	analyze_folder($gameResult);
	record_analysis($gameResult);
}

#AntServer [-HD] [-s seed] [-n number] -p1 player1 -p2 player2
#	-H	Prints this help message.
#	-D	Play a "Duplicate" game.
#	-B	Runs in Debug mode, passing more information about the world state to player code and removing timeouts
#	-s seed	Specify the PRNG seed used to generate the map
#	-p1 player1	Specify the full class for the first player
#	-p2 player2	Specify the full class for the second player
#	-n number	Number of games to play
#	-r filename	Save a replay of all moves to filename
sub run_game {
	my ($gameNumber,$gameId,$number,$baseFolder) = @_;
	my $gameResult = {};
	$gameResult->{id} = $gameId;
	$gameResult->{gameNumber} = $number;
	$gameResult->{baseFolder} = $baseFolder;
	my $tStart = time;
	my $cmdRun = 'java';
	$cmdRun .= ' -ea -Xmx'.($cldebug ? '2048m' : '256m');
	$cmdRun .= ' -cp lib/ants-api.jar:lib/ants-server.jar:ants-zoran/build/libs/ants-zoran.jar';
#	my $cmdRun = 'java -ea -cp lib/ants-api.jar:lib/ants-server.jar:lib/ants-zoran.jar';
	$cmdRun .= ' org/linkedin/contest/ants/server/AntServer';
	$cmdRun .= ' -B' if ($cldebug);
	if ($clseed) {
		$cmdRun .= " -s $clseed";
		$cmdRun .= " -r $resultsFolder/ZoranVsSeed${clseed}.1";
	} else {
		$cmdRun .= " -r $resultsFolder/ZoranVsNothing.$gameResult->{id}";
	}
	$cmdRun .= ' -p1 org.linkedin.contest.ants.zoran.ZoranAnt -p2 org.linkedin.contest.ants.zoran.DoNothingAnt';
	logm("--------------------------------\n");
	logm("Running game $gameNumber: $cmdRun ...\n----\n");
	open(my $ps,"$cmdRun |") || fail("Failed: $!\n");
	my $s = '';
	local $| = 1;
	while (my $line = <$ps>) {
		$s .= $line;
		print $line;
	}
	logm("\n----\ndone\n");
	my $tEnd = time;
	$gameResult->{gameruntime} = $tEnd - $tStart;
	logm("Run time: $gameResult->{gameruntime} s\n");
	$gameResult->{output} = "output_$gameResult->{id}_$gameResult->{gameNumber}.txt";
	logm("Writing '$baseFolder/$gameResult->{output}'\n");
	open (my $fh, ">$baseFolder/$gameResult->{output}") or fail("Can't write server output to '$baseFolder/$gameResult->{output}'");
	print $fh $s;
	close($fh);
	my $player = 0;
	$gameResult->{boardRep} = '';
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
		} elsif ($line=~m/^([^#]*)(#[#\.\%\@]+)$/o) {
			my $boardLine = $2;
			fail("Bad board line representation: $boardLine") unless (length($boardLine) == $boardSize);
			$gameResult->{server}->{boardRep} .= "$boardLine\n";
		}
	}
	my $y = 0;
	$gameResult->{server}->{board}->{bx0} = 0;
	$gameResult->{server}->{board}->{bx1} = $boardSize;
	$gameResult->{server}->{board}->{by0} = 0;
	$gameResult->{server}->{board}->{by1} = $boardSize;
	foreach my $line (split(/\n/,$gameResult->{server}->{boardRep})) {
		for (my $x = 0; $x < $boardSize; $x++) {
			my $c = substr($line,$x,1);
			my $px = $y;
			my $py = $x;
			$gameResult->{server}->{board}->{board}->{$px}->{$py}->{v} = $c;
		}
		$y++;
	}
	$gameResult->{serverpng} = "server_$gameResult->{id}_$gameResult->{gameNumber}.png";
	create_png($gameResult->{server},"$baseFolder/$gameResult->{serverpng}");
	return $gameResult;
}

sub modify_source_code {
	find({ wanted => \&process_file, no_chdir => 1 }, 'ants-zoran/src');
}

sub process_file {
	my $fpath = $_;
	return if (-d $fpath);
	return unless ($fpath=~m/\.java$/o);
	my $contents = '';
	my $modified = 0;
	open(my $fh, "<$fpath") or fail("Can't read $fpath");
	while (my $line = <$fh>) {
		if ($clnolog) {
			if ($line=~m/^[^\/].*Logger\./o) {
				$contents .= "//L$line";
				$modified++;
			} else {
				$contents .= $line unless ($cldry);
			}
		} else {
			if ($line=~m/^\/\/L(.*)$/o) {
				$contents .= "$1\n";
				$modified++;
			} else {
				$contents .= $line unless ($cldry);
			}
		}
	}
	close($fh);
	if ($cldry) {
		return unless (length($contents));
		print "-- $fpath:\n";
		print "$contents\n";
		return;
	}
	return unless ($modified);
	open($fh, ">$fpath") or fail("Can't write $fpath");
	print $fh $contents;
	close($fh);
}

sub record_analysis {
	my ($gameResult) = @_;
	if ($clrun || defined $clsave) {
		# Save summary in archive.txt or adhoc.txt
		my $name = $clsave;
		if (defined $gameResult->{id}) {
			$name = $gameResult->{id};
		} else {
			$name = 'adhoc';
		}
		# run.html
		my $fname = "$resultsFolder/runs/$name.html";
		# full board html
		my $newRow = "<tr>";
		$newRow .= new_td($gameResult->{date},undef);
		$newRow .= new_td($gameResult->{cells}->{percent}.'%',undef);
		my $s = $gameResult->{cells}->{n};
		$s = new_href($s,"file:$gameResult->{gamepng}") if (defined $gameResult->{gamepng});
		$newRow .= new_td($s,undef);
		$s = $gameResult->{rounds};
		$s = new_href($s,"file:$gameResult->{serverpng}") if (defined $gameResult->{serverpng});
		$newRow .= new_td($s, undef);
		$newRow .= new_td($gameResult->{player}->{1}->{food}, undef);
		$newRow .= new_td($gameResult->{player}->{1}->{ants}, undef);
		$newRow .= new_td($gameResult->{gameruntime},undef);
		my $s = '';
		if (defined $gameResult->{rounds} && $gameResult->{rounds} > 0 && defined $gameResult->{gameruntime} && $gameResult->{gameruntime} > 0) {
			$s = sprintf("%.2g", 1000*$gameResult->{gameruntime}/$gameResult->{rounds});
		}
		$newRow .= new_td($s,undef);
		$s = '';
		$newRow .= new_td($s,'red');
		$newRow .= "</tr>\n  ";
		add_html_line($fname,new_run_html($name),$newRow);
	}
}

sub new_run_html {
	my ($name) = @_;
	return <<EOT;
<html><head><title>Run '$name'</title></head>
<body><table border="1" cellspacing="0" cellpadding="3">
  <tr>
    <th>Date</th><th>Fill%</th><th>Cells</th>
    <th>Rounds</th><th>Food</th><th>Ants</th>
    <th>Run time</th><th>Turn time</th><th>Note</th></tr>
  $htmlPlaceholder
</table>
</body></html>
EOT
}

sub new_main_index_html {
	return <<EOT;
<html><head><title>Run results</title></head>
<body><ul>
<li><a href="adhoc.html">Ad hoc</a>
$htmlPlaceholder
</ul>
</body></html>
EOT
}

sub add_html_line {
	my ($fname,$initial,$newLine) = @_;
	my $html = '';
	if (-f $fname) {
		print "Updating $fname\n";
		open (my $fh, "<$fname") or fail("Can't read results html '$fname'");
		local $/ = undef;
		$html = <$fh>;
		close($fh);
	} else {
		print "Creating $fname\n";
		$html = $initial;
	}
	$html=~s/$htmlPlaceholder/$newLine$htmlPlaceholder/go;
	open (my $fh, ">$fname") or fail("Can't write html to '$fname'");
	print $fh $html;
	close($fh);
}

sub create_png {
	my ($stat,$fname) = @_;
	require GD or logm("Can't make PNG, please install GD\n"), return;
	GD->import;
	GD::Image->trueColor(1);
	my $cellSizeX = 6;
	my $cellSizeY = 4;
	my $im = new GD::Image($boardSize * $cellSizeX, $boardSize * $cellSizeY);
	my $emptyBg = $im->colorAllocate(220, 240, 220);
	my $white = $im->colorAllocate(255, 255, 255);
	my $yellow = $im->colorAllocate(255, 245, 220);
	my $black = $im->colorAllocate(0, 0, 0);
	my $red = $im->colorAllocate(255, 0, 0);
	my $green = $im->colorAllocate(0, 255, 0);
	$im->interlaced('true');
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
			elsif ($c eq '.') { $color = $yellow; }
			elsif ($c eq '%') { $color = $green; }
			elsif ($c eq '@') { $color = $red; }
			elsif ($c eq '#') { $color = $black; }
			elsif ($c eq 'N') { $color = $red; $xnest = $px; $ynest = $py; }
			else { fail("check PNG color for '$c'") }
			$im->filledRectangle($cellSizeX * $px, $cellSizeY * $py, $cellSizeX * ($px+1), $cellSizeY * ($py+1), $color);
		}
	}
	if ($xnest>0 && $ynest>0) {
		$im->filledRectangle($cellSizeX * ($xnest-1), $cellSizeY * ($ynest-1), $cellSizeX * ($xnest+2), $cellSizeY * ($ynest+2), $red);
	}
	open (my $fh, ">$fname") or fail("Can't write to '$fname'");
	binmode $fh;
	print $fh $im->png;
	close($fh);
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
	$str .= $text || '&nbsp;';
	$str .= "</td>";
	return $str;
}

sub analyze_folder {
	my ($gameResult) = @_;
	my $folder = $gameResult->{archiveFolder} || $logFolder;
	return unless (defined $folder);
	logm("Analyzing folder '$folder'\n");
	my $t = 0;
	for (my $i = 1; $i <= 50; $i++) {
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
	$gameResult->{missing} = 0;
	$gameResult->{board} = $computed;
	$gameResult->{rounds} = $computed->{turn} unless (defined $gameResult->{rounds});
	if (!defined $gameResult->{player}->{1}->{food}) {
		my $s = `cat $folder/trace_*.txt | grep -c "drops food to 512,512"`;
		$s += 0;
		$gameResult->{player}->{1}->{food} = $s if ($s > 0);
	}
	$gameResult->{time} = $computed->{time};
	$gameResult->{date} = strftime("%Y-%m-%d %H:%M:%S",localtime($computed->{time}));
	if (defined $board->{missing}->{count}) {
		$gameResult->{missing} = $board->{missing}->{count};
	}
	add_stat($gameResult,$computed,'cells','max');
	add_stat($gameResult,$computed,'unknown','max');
	add_stat($gameResult,$computed,'empty','cells');
	add_stat($gameResult,$computed,'obstacle','cells');
	$gameResult->{gamepng} = 'game_';
	if (defined $gameResult->{id}) {
		$gameResult->{gamepng} .= $gameResult->{id}.'_'.$gameResult->{gameNumber};
	} else {
		$gameResult->{gamepng} .= $gameResult->{time};
	}
	$gameResult->{gamepng} .= '.png';
	create_png($gameResult,"$resultsFolder/runs/$gameResult->{gamepng}");
}

sub add_stat {
	my ($res,$computed,$key,$totKey) = @_;
	my $n = $computed->{state}->{$key};
	my $tot = $computed->{state}->{$totKey};
	my $p = '0';
	$p = sprintf("%.2g", $n/$tot*100) if ($tot > 0);
	$res->{$key}->{n} = $n;
	$res->{$key}->{percent} = $p;
}

sub archive_logs {
	my ($gameResult) = @_;
	my $archiveFolder = "$logFolder/archive_".$gameResult->{id}.'_'.$gameResult->{gameNumber};
	mkdir $archiveFolder;
	run_command("mv $logFolder/*.txt $archiveFolder/");
	$gameResult->{archiveFolder} = $archiveFolder;
}

sub compile_project {
	run_command('gradle build');
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
	set_value($b1,$b2,'turn',1);
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
		$res->{antid} = $2;
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
	print "!! @_\n";
	exit 1;
}
