#!/bin/bash
BOOKDIR=../advanced-scala/dist
STEM=advanced-scala
sbt "run $BOOKDIR/$STEM.pdf $BOOKDIR/$STEM-preview.pdf $BOOKDIR/$STEM-preview-with-full-toc.pdf"
open $BOOKDIR/$STEM-preview-with-full-toc.pdf