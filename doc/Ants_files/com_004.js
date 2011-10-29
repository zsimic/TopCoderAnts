AJS.toInit(function ($){
    var editInWord = $('#edit-in-word');
    var pathAuth = false;
    if (!editInWord.length){
       editInWord = $('#edit-in-word-pathauth');
       pathAuth = true;
    }
    if (editInWord.length){
    	 var originalHref = editInWord.attr('href'); 
    	 editInWord.click(function (e) {
              e.preventDefault();
              return doEditInOffice(contextPath, originalHref, 'Word.Document', pathAuth);
          })    	
    }
});


function filterPath(urlPath)
{
    var jsession = getCookie('jsessionid');
    if (!jsession){
        jsession = getCookie('JSESSIONID');
    }
    if (jsession){
        var splitPath = urlPath.split('/');
        var newPath = '';
        for (var i = 0; i < splitPath.length - 1; i++){
            
            if (splitPath[i].length){
                newPath = newPath + '/' + splitPath[i];
            }            
        }
        newPath = newPath + '/ocauth/' + jsession + '/' + splitPath[splitPath.length - 1];
        return newPath        
     }
     else{
        return urlPath
     }
}

function getCookie( check_name ) {
    // first we'll split this cookie up into name/value pairs
    // note: document.cookie only returns name=value, not the other components
    var a_all_cookies = document.cookie.split( ';' );
    var a_temp_cookie = '';
    var cookie_name = '';
    var cookie_value = '';
    var b_cookie_found = false; // set boolean t/f default f

    for ( i = 0; i < a_all_cookies.length; i++ ) {
        // now we'll split apart each name=value pair
        a_temp_cookie = a_all_cookies[i].split( '=' );

        // and trim left/right whitespace while we're at it
        cookie_name = a_temp_cookie[0].replace(/^\s+|\s+$/g, '');

        // if the extracted name matches passed check_name
        if ( cookie_name == check_name ){
            b_cookie_found = true;
            // we need to handle case where cookie has no value but exists (no = sign, that is):
            if ( a_temp_cookie.length > 1 ){
                cookie_value = unescape( a_temp_cookie[1].replace(/^\s+|\s+$/g, '') );
            }
            // note that in cases where cookie is initialized but no value, null is returned
            return cookie_value;
            break;
        }
        a_temp_cookie = null;
        cookie_name = '';
    }
    if ( !b_cookie_found ){
        return null;
    }
}

function getBaseUrl(){    
	return location.protocol + "//" + location.host;	
}

function doEditInOffice(contextPath, webDavUrl, progID, usePathAuth){		
	    var baseUrl = getBaseUrl();
    	if (window.ActiveXObject){
    		var ed; 
    		try	{
    			ed = new ActiveXObject('SharePoint.OpenDocuments.1');    			
    		}
    		catch(err){
    			window.alert('Unable to create an ActiveX object to open the document. This is most likely because of the security settings for your browser.');
    			return false;
    		}
    		if (ed){
    			if (usePathAuth){
    				webDavUrl = filterPath(webDavUrl);
    			}
    			ed.EditDocument(baseUrl + webDavUrl, progID);
    			return false;
    		}
    		else{
    			window.alert('Cannot instantiate the required ActiveX control to open the document. This is most likely because you do not have Office installed or you have an older version of Office.');
    			return false;
    		}  
    	}
    	else if (window.URLLauncher){
    		if (usePathAuth){
				webDavUrl = filterPath(webDavUrl);
			}
    		var wdFile = new URLLauncher();
    		wdFile.open(encodeURI(webDavUrl));
    	}
    	else if(window.InstallTrigger){
    		if(window.confirm('A plugin is required to use this feature. Would you like to download it?')){
    			InstallTrigger.install({'WebDAV Launcher': 'https://update.atlassian.com/office-connector/URLLauncher/latest/webdavloader.xpi'});
    		}
    	}
    	else{
    		window.alert('Firefox or Internet Explorer is required to use this feature.');    		
    	}
    	return false;
}
function enableEdit(node){    
	node.style.cursor='pointer';
	node.style.backgroundColor='#cccccc';
	node.style.color='';
	//node.parentNode.style.border='1px solid #cccccc';
}
function disableEdit(node){
    node.style.cursor='';
	node.style.backgroundColor='#ffffff';
	node.style.color='#ffffff';
	//node.parentNode.style.border='';	
}