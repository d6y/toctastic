#!/bin/bash
BOOKDIR=../essential-play/dist
STEM=essential-play
sbt "run $BOOKDIR/$STEM.pdf $BOOKDIR/$STEM-preview.pdf $BOOKDIR/$STEM-preview-with-full-toc.pdf"
open $BOOKDIR/$STEM-preview-with-full-toc.pdf