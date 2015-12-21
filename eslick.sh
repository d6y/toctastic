#!/bin/bash
BOOKDIR=../essential-slick/dist
STEM=essential-slick-3
sbt "run $BOOKDIR/$STEM.pdf $BOOKDIR/$STEM-preview.pdf $BOOKDIR/$STEM-preview-with-full-toc.pdf"
open $BOOKDIR/$STEM-preview-with-full-toc.pdf
