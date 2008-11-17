<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<title>McIDAS-V User's Guide</title>
<script language="javascript" type="text/javascript">
function setPage(page) {
  var frameElem = document.getElementById('right');
  if (frameElem == null) {
    alert('Cannot get main frame element');
    return(0);
  }
  if (page=='') page = 'toc.html';
  frameElem.src = page;
}
</script>
</head>

<frameset rows="120,*" cols="*" frameborder="NO" border="0" framespacing="0">
  <frame src="mcv-top.html" name="top" scrolling="NO" noresize
      onLoad="setPage('<?php print $_GET["page"]; ?>');">
  <frameset rows="*" cols="300,*" framespacing="0" frameborder="NO" border="0">
    <frame src="frameleft.html" name="left" noresize>
    <frame name="right" id="right"
      style="border-top: 1px solid black; border-left: 1px solid black;"
    >
  </frameset>
</frameset>

<noframes>
<body>
</body>
</noframes>

</html>
