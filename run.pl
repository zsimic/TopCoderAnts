#!/usr/bin/perl

use strict;
use Data::Dumper;

my $boardSize = 512;

my $folder = shift @ARGV;
if (defined $folder) {
	print "Using existing folder '$folder'\n";
} else {
	$folder = 'logs';
	my $cmdCompile = 'javac -d build -cp lib/ants-api.jar:lib/ants-server.jar ants-zoran/src/main/java/org/linkedin/contest/ants/zoran/*.java';
	my $cmdBuild = 'jar cf lib/ants-zoran.jar build/org/';
	my $cmdRun = 'java -ea -cp lib/ants-api.jar:lib/ants-server.jar:ants-zoran/build/libs/ants-zoran.jar';
#	my $cmdRun = 'java -ea -cp lib/ants-api.jar:lib/ants-server.jar:lib/ants-zoran.jar';
	$cmdRun .= ' org/linkedin/contest/ants/server/AntServer';
	$cmdRun .= ' -B -p1 org.linkedin.contest.ants.zoran.ZoranAnt -p2 org.linkedin.contest.ants.zoran.DoNothingAnt';

#	run_command($cmdCompile);
#	run_command($cmdBuild);
	run_command($cmdRun);
	print("finished running game, times: [".join(' ',times)."]\n");
}

my $board = {};
fail("No logs folder generated in '$folder'") unless (-d $folder);
for (my $i = 2; $i <= 33; $i++) {
	my $num = $i;
	$num = "0$num" unless (length($num)>1);
	$board->{scout}->{$num} = read_board("board_${num}_done.txt");
}
$board->{midway} = read_board("board_01_50000.txt");
$board->{final} = read_board("board_01_100000.txt");

#print Data::Dumper->Dump([$board],['board']);
#exit;

$board->{computed} = {name=>'computed', board=>{}};
foreach my $num (sort keys %{$board->{scout}}) {
	join_board($board->{computed}, $board->{scout}->{$num});
}
compare_boards($board->{computed},$board->{final});
compare_boards($board->{final},$board->{computed});
print("all good, times: [".join(' ',times)."]\n");

sub run_command {
	my ($cmd) = @_;
	print "Running: $cmd ... ";
	my $s = `$cmd`;
	if ($?) {
		fail("Can't run command $cmd:\n\nexit code $?\n$!");
	}
	print "done\n";
}

sub compare_boards {
	my ($b1,$b2) = @_;
	foreach my $x (keys %{$b1->{board}}) {
		foreach my $y (keys %{$b1->{board}->{$x}}) {
			my $c1 = $b1->{board}->{$x}->{$y};
			my $c2 = '';
			$c2 = $b2->{board}->{$x}->{$y} if (defined $b2->{board}->{$x}->{$y});
			fail("Board mismatch $b2->{name} vs $b1->{name}: $c2 vs $c1") unless ($c1 == $c2);
		}
	}
}

sub set_cell {
	my ($name,$board,$x,$y,$char) = @_;
#	print "--> n=$name, x=$x y=$y c=$char $board\n";
	my $old = '';
	$old = $board->{$x}->{$y} if (defined $board->{$x}->{$y});
	fail("Mismatch for $name, x=$x y=$y c=$char old=$old") if ($old != $char);
	$board->{$x}->{$y} = $char;
}

sub join_board {
	my ($b1,$b2) = @_;
	foreach my $x (keys %{$b2->{board}}) {
		foreach my $y (keys %{$b2->{board}->{$x}}) {
			my $c = $b2->{board}->{$x}->{$y};
			set_cell("$b1->{name} <- $b2->{name}",$b1->{board},$x,$y,$c);
		}
	}
}

sub read_board {
	my ($name) = @_;
	my $res = {name=>$name};
	my $fname = "$folder/$name";
	fail("No file $fname") unless (-f $fname);
	open(my $fh, "<$fname") or fail("Can't read file $fname");
	my $line = <$fh>;
	if ($line=~m/^([0-9]+) ([0-9]+) xy=\[([0-9]+),([0-9]+)\] bs=\[([0-9]+),([0-9]+)\] f=\[([0-9]+),([0-9]+)\]/o) {
		$res->{turn} = $1;
		$res->{id} = $2;
		$res->{posX} = $3;
		$res->{posY} = $4;
		$res->{bsizeX} = $5;
		$res->{bsizeY} = $6;
		$res->{foodCells} = $7;
		$res->{foodTotal} = $8;
	} else {
		fail("Malformed line 1 in $fname: $line");
	}
	$line = <$fh>;
	if ($line=~m/^Board x=([0-9]+)-([0-9]+) y=([0-9]+)-([0-9]+)$/o) {
		$res->{bx0} = $1;
		$res->{bx1} = $2;
		$res->{by0} = $3;
		$res->{by1} = $4;
	} else {
		fail("Malformed line 2 in $fname: $line");
	}
	$line = <$fh>;
	my $by = $res->{by0};
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
			my $bx = $res->{bx0};
			for (my $i = 0; $i<length($rep); $i++) {
				set_cell($name,$res->{board},$bx++,$by,substr $rep,$i,1);
			}
			$by++;
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
