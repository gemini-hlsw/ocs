# Short term

Something that improves the situation now. Nothing for the future. A dead-end by
design. Something to keep chugging away until we do it right. It shouldn't
take more work than 2 weeks.

1. Code lives in ocs.
1. URLs:
   - Receiver of P1 submissions: phase1.gemini.edu/receive/.../...
   - Viewer of P1 submissions: phase1.gemini.edu/fetch/.../...
1. Each backend a different path like now.
1. Configuration file for each backend (that file is read with every new
   submission). Used for prepending semester to Ids, close deadline, etc.
1. Id counter from previous file names (hashes discussed but discarded).
1. Different backend for joints? If not possible use hard links.
1. Generate PDFs.
1. Stick together the XML and PDFs in tarballs.
1. Send notifications *after* everything is successful. Consult configuration
   file for emails.
1. Multiple p1 versions:
   a. When an XML is sent with an older PIT version, try to migrate to the latest
      p1 model while parsing (as PIT does) and serialize it using the latest
      version.
   b. If there is a problem with the above, just change the Ids and add the
      required tags by parsing directly the XML with an XML parsing library
      (scala-xml + parsing combinators).
1. Proposal viewer. XML stored in different directories with its own set of
   HTTP basic authentication credentials. SSL should still be possible.
   a. Use Jetty to serve both the proposal receiver (the http4s WAR, osgi?) and
      the static files.
   b. Use Apache/NGINX as reverse proxy like now and run the proposal receiver
      on its own (http4s blaze) bound to 127.0.0.1.
1. Deployment: Somehow try to attach it to the release of PIT. Manually: create
   WAR in development machine, copy WAR to phase1.gemini.edu, reload jetty. But
   still not clear that's the easiest or safest release method.


# Long term

1. Code lives in ocs3.
1. Store the proposals in db, not XML. We'd still create XML dumps for 3rd party
   utilities.
1. Create doobie DAOs for current p1 model or make gem support p1 features.
1. Proper authentication: Use Gemini's Active directory.
1. Proper authorization: Proposals with permissions, NGOs/Gemini personnel will
   have their own set granular permissions (roles?).
1. Web forms: configuration stored in db, close deadline by NGO, proposal
   filters, etc.
1. Docker for deployment.
1. Foothold for ITAC backend rewrite.


# To Find out

1. PIT multipart submissions and support in http4s.
1. Will Gem ids be used for phase I in the future?


# Ids

We are open to change the id scheming, but whatever scheme we choose, it should
*look* the same in the long term: i.e ordered vs unordered, alphanumeric vs
numeric only, etc.


## Counter

### Pros

- Similar to what we have now. Prevents unforeseeable issues.
- The id has some meaning: proposal 2 arrived right after proposal 1.


### Cons

- Have to deal with storage persistency.
- Have to deal with shared data in what otherwise would be totally independent
  concurrent proposal submission requests.

## Hashes

 - Non cryptographic hash like Murmurhash (available in all major languages and
   easy to implement if not available). 32 bits length should be good enough to
   avoid collisions (TODO: calculate exact probability to be certain).
 - Encoding 32 bit hashes:
   - Base58: only numbers and printed characters that can't be mistakenly
     mistyped. Examples:
     - 3fq91a
     - 3fmnCW
     - 2SJWB1
   - Base10, only numbers. Examples:
     - 4294967296
     - 2147483647


### Pros

- Deterministic. Id intrinsic to the data coming with the proposal. It can be
  calculated cheaply anywhere without depending in anything else. No need to
  persist anything to disk or share data between 2 simultaneous requests.
- True identity. If 2 proposals have the same id they are the same proposal. No
  need to care about accidental misassignments of ids due to HTTP resubmissions
  overwrites or whatnot. The proposal receiver endpoint would be truly
  [idempotent](http://www.restapitutorial.com/lessons/idempotency.html) which
  allows to forget about having to deal with the inherent annoyances of
  non-idempotent HTTP methods.


### Cons

- Id will change if content of proposal is modified.
- Characters in the id may create problems in 3rd party scripts.
- If only numbers are used the ids may be too long.
