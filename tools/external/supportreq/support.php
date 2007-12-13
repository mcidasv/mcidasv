<?php
require('class.phpmailer.php');

#$SUPPORT_EMAIL = 'EMAIL@YOUR.SUPPORT.TEAM.COM';
#$SUPPORT_NAME = 'Team Name';
$SUPPORT_TEAM = 'mug@ssec.wisc.edu';
$SUPPORT_NAME = 'MUG Team';

$data = $GLOBALS['HTTP_POST_VARS']['form_data'];
$files = $GLOBALS['HTTP_POST_FILES']['form_data'];

$mail = new PHPMailer();

$mail->isMail();
$mail->From = $data['email'];
$mail->FromName = $data['fromName'];
$mail->AddAddress($SUPPORT_EMAIL, $SUPPORT_TEAM);
$mail->Subject = $data['subject'];
$mail->Body = $data['description'];
$mail->WordWrap = 80;

if ($files['name']['att_one'] != '')
  $mail->AddAttachment($files['tmp_name']['att_one'], $files['name']['att_one'], 'base64', $files['type']['att_one']);

if ($files['name']['att_two'] != '')
  $mail->AddAttachment($files['tmp_name']['att_two'], $files['name']['att_two'], 'base64', $files['type']['att_two']);

if ($files['name']['att_three'] != '')
  $mail->AddAttachment($files['tmp_name']['att_three'], $files['name']['att_three'], 'base64', $files['type']['att_three']);

if (!$mail->Send()) {
  echo "Message was not sent. :(<br/>\n";
  echo 'Error: ' . $mail->ErrorInfo;
} else {
  echo "your email has been sent";
}
?>
