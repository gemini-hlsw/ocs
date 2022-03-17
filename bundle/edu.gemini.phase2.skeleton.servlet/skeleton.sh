#!/bin/bash

# Set the host to your site's test ODB.
HOST=localhost

#
# USAGE
#
# skeleton myproposal.xml [myproposal.pdf] [myproposal2.pdf] ... [program id]
#
#
# DESCRIPTION
#
# Use this script to post a proposal XML to the ODB, creating a corresponding
# skeleton.
#
# Text Attachment PDF(s)
#
# Any number of proposal attachments may be included.  At least one must be
# specified unless there is a single proposal attachment with the same name
# as the proposal file, changing the `.xml` extension for `.pdf`.
#
# For example, if the proposal is "myproposal.xml" and its text attachment is
# "myproposal.pdf" in the same directory there's no need to specify it.
# Regardless, the attachment PDF(s) must have a .pdf extension.
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
# Post a proposal for which the PDF attachment has a different name and no
# proposal id has been assigned by ITAC software.
#
#	skeleton myproposal.xml mytextpart.pdf GS-2012B-Q-2
#
# Post a proposal with two PDF attachments and no proposal ID.
#
#	skeleton myproposal.xml myattachment1.pdf myattachment2.dpf GS-2022B-Q-1
#

CMD=$0

fail() {
    echo "$1"
    echo "Usage: $CMD proposal.xml [attachment1.pdf] [attachment2.pdf] ... [programid]" 1>&2
    exit 1
}

verifyId() {
    echo "$1" | grep -q "^G[NS]-[0-9][0-9][0-9][0-9][AB]-[A-Z]*-[0-9]*$" || fail "$1 doesn't appear to be a Gemini program ID"
}

XML=
PDFS=()
ID=

# Process the command line args to find the proposal XML, attachment PDF and
# program id if specified.
while [ $# -gt 0 ]; do
    case "$1" in
        *".xml" ) XML=$1 ;;
        *".pdf" ) PDFS+=("$1") ;;
        *) ID=$1
           verifyId $ID
           ;;
    esac
    shift
done

# Make sure we have a proposal file and that it exists
[ -n "$XML" ] || fail "Missing program.xml argument."
[ -f "$XML" ] || fail "Proposal file $XML does not exist."

# If a pdf file is not explicitly specified, try the same prefix as the
# proposal file but with a ".pdf" extension.
if [ ${#PDFS[@]} -eq 0 ]; then
    DEF_PDF=`echo "$XML" | sed -e 's/\.xml$/\.pdf/'`
    PDFS+=("$DEF_PDF")
fi

# Validate that all PDFS exist.
for p in "${PDFS[@]}"; do
    if [ ! -f "$p" ]; then
        fail "Proposal attachment $p does not exist."
    fi
done

# Build the curl command to execute.
CURL="curl -F \"proposal=@$XML;type=text/xml\""

# If there is just one PDF, send with the "attachment" id.
# Otherwise, add each one with its (1-based) "attachment" index.
if [ ${#PDFS[@]} -eq 1 ]; then
    PDF=${PDFS[0]}
    CURL+=" -F \"attachment=@$PDF;type=application/pdf\""
else
    for (( i=0; i<${#PDFS[@]}; i++ )); do
        PDF=${PDFS[$i]}
        IDX=`expr $i + 1`
        CURL+=" -F \"attachment$IDX=@$PDF;type=application/pdf\""
    done
fi

# Add the program ID if specified.
if [ -n "$ID" ]; then
    CURL+=" -F \"id=$ID\""
fi

# Finally add the skeleton host and arguments.
CURL+=" http://${HOST}:8442/skeleton?convert=true"

eval $CURLXS

