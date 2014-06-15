
all:	system.png system.svg

%.png:	%.dot
	mkdir -p resources/architecture
	dot -Tpng -oresources/architecture/$@ $<

%.svg:	%.dot
	mkdir -p resources/architecture
	dot -Tsvg -oresources/architecture/$@ $<
