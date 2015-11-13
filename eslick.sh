#!/bin/bash
BOOKDIR=../essential-slick/dist
STEM=essential-slick-3
mv -v $BOOKDIR/$STEM-preview.pdf $BOOKDIR/$STEM-preview-raw.pdf
sbt "run $BOOKDIR/$STEM.pdf $BOOKDIR/$STEM-preview-raw.pdf $BOOKDIR/$STEM-preview.pdf"
rm $BOOKDIR/$STEM-preview-raw.pdf
open $BOOKDIR/$STEM-preview.pdf
