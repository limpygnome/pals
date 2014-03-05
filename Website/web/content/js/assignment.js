txtElems = new Array();

function fetchElems()
{
	return document.all.tags('textarea');
}
function assLoad()
{
	var elems  = fetchElems();
	for(var i = 0; i < elems.length; i++)
		txtElems[i] = elems[i].value;
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
