
## edu.gemini.wdba.xmlrpc.server

XML RPC Server

### Example XML RPC call

The OT and seqexec will post XML RPC docs and get XML responses.  This can also be done from the command line of course.

```
$ cat req.xml
<?xml version="1.0"?>
<methodCall>
  <methodName>WDBA_Session.getObservationsAndTitles</methodName>
  <params>
    <param>
      <value>sessionQueue</value>
    </param>
  </params>
</methodCall>

$ curl --data @req.xml http://gsodbtest:8442/wdba
<?xml version="1.0" encoding="UTF-8"?><methodResponse xmlns:ex="http://ws.apache.org/xmlrpc/namespaces/extensions"><params><param><value><array><data><value><struct><member><name>OBS_ID</name><value>GS-ENG20210827-5</value></member><member><name>TITLE</name><value>GMOS Grating tilt LUT check</value></member></struct></value></data></array></value></param></params></methodResponse>
```

### Provenance

This bundle originated from `edu.gemini.wdba.xmlrpc.server` in the OCS 1.5 build. It subsumes the following OCS 1.5 bundles, which no longer exist on their own.
 
- `edu.gemini.wdba.exec.impl`
- `edu.gemini.wdba.session.impl`
- `edu.gemini.wdba.tcc.impl`
- `edu.gemini.wdba.glue`
- `edu.gemini.wdba.glue.api`
