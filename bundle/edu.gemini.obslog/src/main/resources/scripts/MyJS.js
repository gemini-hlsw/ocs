function CommentConn(observationID, commentID, comment) {
    var xmlhttp, bComplete = false;
    //this.observationID = observationID;
    //this.commentID = commentID;
    //this.comment = comment;

    try {
        xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
    }
    catch (e) {
        try {
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
        }
        catch (e) {
            try {
                xmlhttp = new XMLHttpRequest();
            }
            catch (e) {
                xmlhttp = false;
            }
        }
    }
    if (!xmlhttp) return null;

    this.connect = function(sURL, fnDone) {
        if (!xmlhttp) return false;

        bComplete = false;
        try {
            xmlhttp.open("POST", sURL, true);
            xmlhttp.setRequestHeader("Method", "POST " + sURL + " HTTP/1.1");
            xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            xmlhttp.onreadystatechange = function() {
                if (xmlhttp.readyState == 4 && !bComplete) {
                    bComplete = true;
                    //document.getElementById('test_content').innerHTML = '<strong>Done!</strong>';
                    fnDone(xmlhttp, commentID);
                } else {
                    //alert(xmlhttp.readyState);
                }
            }
            var sVars = "observationID=" + observationID + "&configID=" + commentID + "&comment=" + encodeURIComponent(comment);
            xmlhttp.send(sVars);
        } catch(z) {
            Alert(z);
            return false;
        }
        return true;
    };
    return this;
}

function handleHttpResponse(oXML, commentID) {
    if (oXML == null) {
        alert("Null HTTP response received!");
        return;
    }
    restoreCommentArea(commentID);
}

function restoreCommentArea(commentID) {
    var textEl = document.getElementById(commentID);
    textEl.rows = countLines(textEl.value);

    var sbuttonName = "sbutton" + commentID;
    var buttonEl = document.getElementById(sbuttonName);
    if (buttonEl == null) {
        alert("Button: " + sbuttonName + " not found.");
        return;
    }
    buttonEl.parentNode.removeChild(buttonEl);
}

function countLines(comment) {
    var lineCount = 0;

    var result = comment.match(/$/gm);
    if (result != null) {
        lineCount = result.length;
        //alert(lineCount);
    }
    return lineCount;
}

function updateComment(observationID, commentID) {
    var textEl = document.getElementById(commentID);
    var comment = textEl.value;

    var myConn = new CommentConn(observationID, commentID, comment);
    if (!myConn) {
        alert("XmlHTTP not available.  No commenting is possible.  Better browser needed.");
        return;
    }

    myConn.connect("updateComment.action", handleHttpResponse);

    var myFader = new Fader(textEl, "FFFF99", "FFFFFF");
    myFader.fade();

    //textEl.rows = countLines(comment);
}

function growText(observationID, commentID) {
    var EXPAND_ROWS = 8;
    var textEl = document.getElementById(commentID);

    try {
        if (textEl == null) {
            alert("Comment text area: " + commentID + " not found");
            return;
        }
        // This ensures that the button won't be added twice
        if (textEl.rows == EXPAND_ROWS) return;

        // Create a button for saving
        var parent = textEl.parentNode;
        var saveButton = document.createElement("input");
        saveButton.value = "save comment";
        saveButton.type = "button";
        saveButton.id = "sbutton" + commentID;
        saveButton.onclick = function() {
            updateComment(observationID, commentID);
        }
        parent.appendChild(saveButton);

        // Expand the box
        textEl.rows = EXPAND_ROWS;
    } catch(z) {
        alert(z);
    }
}

function sendText(commentID, button) {
    var textEl = document.getElementById(commentID);

    var parent = button.parentNode;
    parent.removeChild(button);
}