var runningAJAX=0;
var framesLoaded=0;
var currentPage=null;
var loadPage=null;

function canRun() {
  if (!document.getElementById) return(false);
  else return(true);
}

function runAJAX(filename, postvars, targetdiv, donefunc) {
  if (!canRun()) return(false);
  if (postvars==null) postvars='';
  var xmlhttp;
  if(window.XMLHttpRequest) {
    xmlhttp = new XMLHttpRequest();
  }
  else if(window.ActiveXObject) {
    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
  }
  else { alert('AJAX HTTP not supported'); }

  if (postvars.length>0 || filename.match('.php')) {
    xmlhttp.open('POST', filename, true);
    xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xmlhttp.setRequestHeader("Content-length", postvars.length);
    xmlhttp.setRequestHeader("Connection", "close");
    xmlhttp.send(postvars);
  }
  else {
    xmlhttp.open('GET', filename, true);
    xmlhttp.setRequestHeader("Connection", "close");
    xmlhttp.send(null);
  }
  xmlhttp.onreadystatechange=function() {
    if (xmlhttp.readyState==4) {
      if (xmlhttp.status==200) {
        var theElem=document.getElementById(targetdiv);
        if (theElem!=null) theElem.innerHTML=xmlhttp.responseText;
        else donefunc=null;
        if (donefunc!=null) eval(donefunc);
      }
      else { alert('AJAX: HTTP error '+xmlhttp.status); }
      runningAJAX--;
      if (runningAJAX<0) { runningAJAX=0; }
    }
  }
  try { xmlhttp.send(null); }
  catch (err) { }
  return(false);
}

function initPage(page) {
  if (page==null || page=='') page='toc.html';
  loadPage=page;
  loadTop();
  loadLeft();
}

function setPage(page, fromPage) {
  setMenu(page);
  setBookmark(page);
  if (fromPage==null) {
    loadRight(page);
  }
}

function setMenu(page) {
  var rootElem=document.getElementById('div_index');
  if (rootElem==null) return;

  var leafList=rootElem.getElementsByTagName('span');
  for (var i=0; i<leafList.length; i++) {
    var leaf=leafList[i];
    var leafFunc=leaf.onclick+'';
    if (leafFunc=='') continue;
    var target=extractSetPage(leafFunc);
    if (target=='') continue;
    if (target==page) {

      // Expand the tree to this point
      var parentElem=leaf.parentNode;
      while (parentElem!=null && parentElem.id!=rootElem.id) {
        var parentDisclose=getPreviousByClass(parentElem,'toggle');
        showChildren(parentDisclose, parentElem);
        parentElem=parentElem.parentNode;
      }

      // Set the leaf to selected
      if (currentPage!=null)
        try{unselect(currentPage)}catch(err){alert(err)};
      currentPage=leaf;
      try{select(leaf)}catch(err){};

      break;
    }
  }
}

function getPreviousByClass(sibling, byclass) {
  if (sibling==null) return null;
  var previous=sibling.previousSibling;
  for (var j=0; j<10; j++) {
    if (previous==null) continue;
    if (previous.className==byclass) break;
    previous=previous.previousSibling;
  }
  if (j==10) {
//    alert('Failed to find previous sibling: '+byclass);
    return null;
  }
  return previous;
}

function extractSetPage(functionString) {
  var lines=functionString.split(/\n/);
  var line=lines.pop();
  if (line==null) return(null);
  while (!line.match('setPage')) {
    line=lines.pop();
    if (line==null) return(null);
  }
  var page=line.replace(/.*setPage\(["\']/,'');
  page=page.replace(/["\'].*/,'');
  return(page);
}

function setBookmark(page) {
  var elem=document.getElementById('a_bookmark');
  if (elem!=null) elem.href='index.php?page='+page;
}

function doSearching() {
  if (!canRun()) return(false);
  var elem=document.getElementById('div_results');
  if (elem!=null) elem.innerHTML="Searching...";
}

function setView(page) {
  if (!canRun()) return(false);
  switch (page) {
  case 'index':
    document.getElementById('tab_index').className='selected';
    document.getElementById('div_index').style.display='block';
    document.getElementById('tab_search').className='unselected';
    document.getElementById('div_search').style.display='none';
    document.getElementById('tab_pdf').className='unselected';
    document.getElementById('div_pdf').style.display='none';
    break;
  case 'search':
    document.getElementById('tab_index').className='unselected';
    document.getElementById('div_index').style.display='none';
    document.getElementById('tab_search').className='selected';
    document.getElementById('div_search').style.display='block';
    document.getElementById('tab_pdf').className='unselected';
    document.getElementById('div_pdf').style.display='none';
    break;
  case 'pdf':
    document.getElementById('tab_index').className='unselected';
    document.getElementById('div_index').style.display='none';
    document.getElementById('tab_search').className='unselected';
    document.getElementById('div_search').style.display='none';
    document.getElementById('tab_pdf').className='selected';
    document.getElementById('div_pdf').style.display='block';
    break;
  default:
    alert('Unknown view: '+view);
  }
}

function toggleChildren(elemSelf,elemId) {
  if (!canRun()) return(false);
  var elem = document.getElementById(elemId);
  if (elem.style.display=='') {
    elem.style.display='none';
  }
  if (elem.style.display=='none') {
    showChildren(elemSelf, elem);
  }
  else {
    hideChildren(elemSelf, elem);
  }
}

function showChildren(elemSelf, elem) {
  if (!canRun()) return(false);
  if (elemSelf==null || elem==null) return(false);
  elemSelf.innerHTML='-';
  elem.style.display='block';
}

function hideChildren(elemSelf, elem) {
  if (!canRun()) return(false);
  if (elemSelf==null || elem==null) return(false);
  elemSelf.innerHTML='+';
  elem.style.display='none';
}

function doSearch() {
  var elem=document.getElementById('input_search');
  if (elem==null) return;
  doSearchTerm(elem.value);
}

function doSearchTerm(string) {
  if (runningAJAX>0 || string=='') return;
  doSearching();
  runAJAX('mcv-search.php', 'terms='+string, 'div_results');
}

function loadTop() {
  runAJAX('mcv-top.php', '', 'top','topLoaded()');
}
function loadLeft() {
  runAJAX('mcv-menu.php', '', 'left','leftLoaded()');
}
function loadRight(filename) {
  var elem=document.getElementById('frame_right');
  if (elem==null) return;
  elem.src=filename;
//  runAJAX(filename, '', 'right','rightLoaded(\''+filename+'\')');
}

function topLoaded() {
  if (++framesLoaded==2) {
    fixIE();
    setPage(loadPage);
  }
}

function leftLoaded() {
  var left=document.getElementById('div_index');
  if (left==null) return;
  var spans=left.getElementsByTagName('span');
  for (var i=0; i<spans.length; i++) {
    var span=spans[i];
    if (span.className!='link') continue;
    span.onmouseover = new Function('try{hilite(this)}catch(err){};');
    span.onmouseout = new Function('try{unhilite(this)}catch(err){};');
  }
  if (++framesLoaded==2) {
    fixIE();
    setPage(loadPage);
  }
}

function rightLoaded() {
}

function hilite(elem) {
  if (elem.selected==null || elem.selected==0)
    elem.className='link hilited';
  else
    elem.className='link hilited selected';
}
function unhilite(elem) {
  if (elem.selected==null || elem.selected==0)
    elem.className='link';
  else
    elem.className='link selected';
}
function select(elem) {
  elem.className='link selected';
  elem.selected=1;
}
function unselect(elem) {
  elem.className='link';
  elem.selected=0;
}

// There is a rendering problem with IE:
// When the page first loads, the right div/frame does not render correctly
//  until the page is resized.  Attempt to do that here programmatically.
function fixIE() {
  if (typeof(window.innerWidth)=='number') return;
  if (document.body) {
    window.resizeBy(-1,0);
    window.resizeBy(1,0);
  }
}
