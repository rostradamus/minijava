#!/bin/bash
rm -f TokenMgrError.java ParseException.java Token.java SimpleCharStream.java
javacc X86_64SimParser.jj
