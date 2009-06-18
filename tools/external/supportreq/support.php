<?php
require('class.phpmailer.php');

if (isset($_POST['form_data'])) {
  $data = $_POST['form_data'];
}
else {
  echo "Message was not sent. :(<br/>\n";
  echo 'Error: form_data not set';
  exit(1);
}
if (isset($_FILES['form_data'])) {
  $files = $_FILES['form_data'];
}
else {
  $files = array();
}

$mail = new PHPMailer();

$mail->isMail();
$mail->From = $data['email'];
$mail->FromName = $data['fromName'];
$mail->AddAddress('mug@ssec.wisc.edu', 'MUG Team');
$mail->Subject = "[Org=" . $data['organization'] . "]: " . $data['subject'];
$mail->Body = $data['description'];
$mail->WordWrap = 80;

$sendcc = isset($data['cc_user']);
if (($sendcc == false) || ($data['cc_user'] == "true")) {
  $mail->AddAddress($data['email'], $data['fromName']);
}

if (isset($files['name']['att_one']) && $files['name']['att_one'] != '') {
  $mail->AddAttachment($files['tmp_name']['att_one'], $files['name']['att_one'], 'base64', $files['type']['att_one']);
}

if (isset($files['name']['att_two']) && $files['name']['att_two'] != '') {
  $mail->AddAttachment($files['tmp_name']['att_two'], $files['name']['att_two'], 'base64', $files['type']['att_two']);
}

if (isset($files['name']['att_three']) && $files['name']['att_three'] != '') {
  $mail->AddAttachment($files['tmp_name']['att_three'], $files['name']['att_three'], 'base64', $files['type']['att_three']);
}

if (isset($files['name']['att_extra']) && $files['name']['att_extra'] != '') {
  $mail->AddAttachment($files['tmp_name']['att_extra'], $files['name']['att_extra'], 'base64', $files['type']['att_extra']);
}

if (isset($files['name']['att_state']) && $files['name']['att_state'] != '') {
  $mail->AddAttachment($files['tmp_name']['att_state'], $files['name']['att_state'], 'base64', $files['type']['att_state']);
}

if (isset($files['name']['att_log']) && $files['name']['att_log'] != '') {
  $mail->AddAttachment($files['tmp_name']['att_log'], $files['name']['att_log'], 'base64', $files['type']['att_log']);
}

if (!$mail->Send()) {
  echo "Message was not sent. :(<br/>\n";
  echo 'Error: ' . $mail->ErrorInfo;
} else {
  echo "your email has been sent";
}
?>
