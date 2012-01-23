#!/usr/bin/env perl -w

my @files = <*_network.dat>;
foreach my $file (@files) {
	$file =~ s/_network.dat//;
	
	open(DGS, ">$file.dgs");
	open(NET, $file."_network.dat") or die($!);
	open(COM, $file."_community.dat") or die($!);
	
	print DGS <<EOF;
DGS003
$file.dgs 0 0
EOF

	while (<COM>) {
		if (/(\d+)\s+(\d+)/) {
			print DGS "an $1 value=$2\n";
		}
	}
	
	while (<NET>) {
		if (/^(\d+)\s+(\d+)(\s+(\d+\.?\d*))?/) {
			print DGS "ae \"".$1."-".$2."\" $1 $2";
			print DGS " weight=$4" if (defined $4);
			print DGS "\n";
		}
	}
	close(DGS);
	close(NET);
	close(COM);
}