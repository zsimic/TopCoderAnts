#!/usr/bin/perl

use strict;
use File::Basename;
use POSIX qw(strftime);
use Getopt::Long;
use Data::Dumper;

my $boardSize = 512;
my $logFolder = 'logs';
my $resultsFolder = 'results';
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
my $clarchive = 0;
my $clfolder = undef;
my $clhelp = 0;
Getopt::Long::Configure ("bundling");
GetOptions(
	'c|compile'=>\$clcompile,
	'r|run=i'=>\$clrun,
	's|save:s'=>\$clsave,
	'a|archive'=>\$clarchive,
	'f|folder=s'=>\$clfolder,
	'h|?|help'=>\$clhelp
) or usage();
usage() if ($clhelp);
$clcompile = 1 if ($clcompile);
$clarchive = 1 if ($clarchive);
#print "c=[$clcompile] r=[$clrun]\n"; exit;
usage("-a can be specified only with -r") if ($clarchive and !$clrun);
usage("-f can't be specified with -r") if (defined $clfolder and $clrun);

if ($clrun) {
	compile_project() if ($clcompile);
	my $n = $clrun;
	while ($n--) {
		my $elapsed = run_game();
		my $folder = $logFolder;
		$folder = archive_logs() if ($clarchive);
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
	if (!defined $clsave) {
		print $res->{summary};
		print "--------\n" if ($clrun);
	} else {
		my $name = $clsave;
		$name = $res->{folder} unless (length($name));
		if ($name eq $logFolder) {
			$name = 'adhoc';
		} else {
			$name = basename($name);
		}
		$name = "$resultsFolder/$name.txt";
		logm("Recording analysis to $name ... ");
		open (my $fh, ">>$name");
		print $fh $res->{summary};
		print $fh "--------\n";
		close($fh);
		logm("done\n");
	}
	if ($clrun) {
		my $fname = "$resultsFolder/runs.txt";
		my $fh;
		if (-f $fname) {
			open ($fh, ">>$fname") or fail("Can't write results to '$fname'");
		} else {
			open ($fh, ">$fname") or fail("Can't write results to '$fname'");
			print $fh "date\tmissing\tfill%\tcells\tgameruntime\tfolder\tfail\n";
		}
		print $fh "$res->{date}\t$res->{missing}\t$res->{cells}->{percent}\t$res->{cells}->{n}\t$res->{gameruntime}\t$res->{folder}\t$res->{fail}\n";
		close($fh);
	}
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
	foreach my $x (keys %{$b2->{board}}) {
		foreach my $y (keys %{$b2->{board}->{$x}}) {
			my $c = $b2->{board}->{$x}->{$y}->{v};
			set_cell($b1,$x,$y,$c,$b2->{name}) if ($c ne ' ');
			set_value($b1,$b2,'bx0',-1);
			set_value($b1,$b2,'bx1',1);
			set_value($b1,$b2,'by0',-1);
			set_value($b1,$b2,'by1',1);
			set_value($b1,$b2,'turn',1);
			set_value($b1,$b2,'bsizeX',1);
			set_value($b1,$b2,'bsizeY',1);
		}
	}
}

sub set_value {
	my ($b1,$b2,$key,$direction) = @_;
	return unless (defined $b2->{key});
	if ($direction == 0) {
		$b1->{$key} = $b2->{key} if ($b1->{key} ne $b2->{key});
	} else {
		my $old = $direction * $boardSize * 2;
		$old = $b1->{$key} if (defined $b1->{key});
		$b1->{$key} = $b2->{key} if ($old * $direction < $b2->{key} * $direction);
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
