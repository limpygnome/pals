txtElems = new Array();

function fetchElems()
{
	return document.getElementsByTagName('textarea');
}
function fetchHyps()
{
	return document.getElementsByTagName('a');
}
function assLoad()
{
	// Log text areas to monitor for changes
	var elems  = fetchElems();
	for(var i = 0; i < elems.length; i++)
		txtElems[i] = elems[i].value;
	// Enable event to prevent leaving the page by hooking each hyperlink click
	var hyps = fetchHyps();
	for(var i = 0; i < hyps.length; i++)
	{
		hyps[i].addEventListener('click',
		function(event)
		{
			if(!assLeave())
			{
				event.preventDefault();
				event.stopPropagation();
			}
		},
		true);
	}
}
function assLeave()
{
	var modified = false;
	var elems = fetchElems();
	if(elems.length != txtElems.length)
		modified = true;
	else
	{
		for(var i = 0; i < elems.length; i++)
		{
			if(elems[i].value != txtElems[i])
				modified = true;
		}
	}
	return modified ? window.confirm("You have not saved your questions; continue?") : true;
}
