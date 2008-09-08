<?php
require('class.phpmailer.php');

$data = $GLOBALS['HTTP_POST_VARS']['form_data'];
$files = $GLOBALS['HTTP_POST_FILES']['form_data'];

$mail = new PHPMailer();

$mail->isMail();
$mail->From = $data['email'];
$mail->FromName = $data['fromName'];
$mail->AddAddress('mug@ssec.wisc.edu', 'MUG Team');
$mail->Subject = $data['subject'];
$mail->Body = $data['description'];
$mail->WordWrap = 80;

$sendcc = isset($data['cc_user']);
if (($sendcc == false) || ($data['cc_user'] == "true")) {
  $mail->AddAddress($data['email'], $data['fromName']);
}

if ($files['name']['att_one'] != '')
  $mail->AddAttachment($files['tmp_name']['att_one'], $files['name']['att_one'], 'base64', $files['type']['att_one']);

if ($files['name']['att_two'] != '')
  $mail->AddAttachment($files['tmp_name']['att_two'], $files['name']['att_two'], 'base64', $files['type']['att_two']);

if ($files['name']['att_three'] != '')
  $mail->AddAttachment($files['tmp_name']['att_three'], $files['name']['att_three'], 'base64', $files['type']['att_three']);

if ($files['name']['att_extra'] != '')
  $mail->AddAttachment($files['tmp_name']['att_extra'], $files['name']['att_extra'], 'base64', $files['type']['att_extra']);

if ($files['name']['att_state'] != '')
  $mail->AddAttachment($files['tmp_name']['att_state'], $files['name']['att_state'], 'base64', $files['type']['att_state']);

// print_r($mail);
// var_dump(isset($data['cc_user']));

if (!$mail->Send()) {
  echo "Message was not sent. :(<br/>\n";
  echo 'Error: ' . $mail->ErrorInfo;
} else {
  echo "your email has been sent";
}
?>
