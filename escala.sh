#!/bin/bash
BOOKDIR=../essential-scala/dist
STEM=essential-scala
sbt "run $BOOKDIR/$STEM.pdf $BOOKDIR/$STEM-preview.pdf $BOOKDIR/$STEM-preview-with-full-toc.pdf"
open $BOOKDIR/$STEM-preview-with-full-toc.pdf