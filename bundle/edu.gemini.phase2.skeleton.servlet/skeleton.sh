#!/bin/bash

# Set the host to your site's test ODB.
HOST=localhost

#
# USAGE
#
# skeleton myproposal.xml [myproposal.pdf] [program id]
#
#
# DESCRIPTION
# 
# Use this script to post a proposal XML to the ODB, creating a corresponding
# skeleton.
#
# Text Attachment PDF
#
# The proposal text attachment must be specified unless it is in the same
# directory as the XML file and has the same file name (except with a .pdf
# extension).  For example, if the proposal is "myproposal.xml" and its text
# attachment is "myproposal.pdf" in the same directory there's no need to
# specify it.  Regardless, the attachment PDF must have a .pdf extension.
#
# Program Ids
#
# Every proposal document either has to contain a Gemini program ID (as
# assigned by the ITAC software) or else you must specify the program ID on
# the command line.  Program ids are checked for validity so 'normal' ids 
# like GS-2012B-Q-1 will work but random strings or exotic program types like
# GS-2012B-TEST-1 will not.
#
# If you choose a program id that already exists in the database, the existing
# program will be replaced with the skeleton unless it has one or more
# observations beyond Phase 2 status.
#
#
# EXAMPLES
#
# Post a proposal created by the PIT but not yet assigned an ID with ITAC.
# Assumes that there is a myproposal.pdf file in the cwd.
#
#	skeleton myproposal.xml GS-2012B-Q-1
#
# Post a proposal that contains an assigned ID (and for which myproposal.pdf
# exists)
#
#	skeleton myproposal.xml
#
# Post a proposal for which the text attachment has a different name and no
# proposal id has been assigned by ITAC software.
#
#	skeleton myproposal.xml mytextpart.pdf GS-2012B-Q-2
#
#

CMD=$0

fail() {
    echo "$1"
    echo "Usage: $CMD proposal.xml [proposal.pdf] [programid]" 1>&2
    exit 1
}

verifyId() {
    echo "$1" | grep -q "^G[NS]-[0-9][0-9][0-9][0-9][AB]-[A-Z]*-[0-9]*$" || fail "$1 doesn't appear to be a Gemini program ID"
}
 
XML=
PDF=
ID=

# Process the command line args to find the proposal XML, attachment PDF and
# program id if specified.
while [ $# -gt 0 ]; do
    case "$1" in
        *".xml" ) XML=$1 ;;  
        *".pdf" ) PDF=$1 ;;
        *) ID=$1
           verifyId $ID
           ;;
    esac
    shift
done

# Make sure we have a proposal file and that it exists
[ -n "$XML" ] || fail "Missing program.xml argument."
[ -f "$XML" ] || fail "Proposal file $XML does not exist."

# If the pdf file is not explicitly specified, try the same prefix as the
# proposal file but with a ".pdf" extension.
ORIG_PDF=$PDF
if [ -z "$PDF" ]; then
   PDF=`echo "$XML" | sed -e 's/\.xml$/\.pdf/'`
fi
if [ ! -f "$PDF" ]; then
   [ -n "$ORIG_PDF" ] || fail "Proposal attachment not specified and default $PDF doesn't exist."
   fail "Proposal attachment $PDF does not exist."
fi

if [ -z "$ID" ]; then
    curl -F "proposal=@$XML;type=text/xml" -F "attachment=@$PDF;type=application/pdf" http://${HOST}:8442/skeleton?convert=true
else
    curl -F "proposal=@$XML;type=text/xml" -F "attachment=@$PDF;type=application/pdf" -F "id=$ID" http://${HOST}:8442/skeleton?convert=true
fi

