make : 
CREATE TABLE ifm_app_user.impl_email_template ( id serial4 NOT NULL, "type" varchar(50) NOT NULL, email_subject varchar(200) NOT NULL, email_body text NOT NULL, created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, CONSTRAINT impl_email_template_pkey PRIMARY KEY (id) );

INSERT INTO ifm_app_user.impl_email_template
(id, "type", email_subject, email_body, created_at, updated_at)
VALUES(1, 'CWI_ALERT', 'New CWI Alert Notification - [Alert_ID]', '<html>
  <body>
    <p>Dear [Recipient],</p>
    <p>There is a new Alert with the following details:</p>
    <ul>
      <li><strong>Transaction Date & Time:</strong> [Transaction_date]</li>
      <li><strong>Alert ID:</strong> [Alert_ID]</li>
      <li><strong>Customer Party ID:</strong> [Party_id]</li>
      <li><strong>Customer Name:</strong> [Customer_name]</li>
      <li><strong>Transaction Type:</strong> [Transaction_type]</li>
      <li><strong>Amount:</strong> [Transaction_amount]</li>
      <li><strong>Score:</strong> [Score]</li>
      <li><strong>Channel Type:</strong> [Channel_type]</li>
    </ul>
    <p>This is an automated notification from the Actimize Anti-Fraud System. Please do not reply to this email.</p>
  </body>
</html>', '2025-08-01 15:36:13.078', '2025-08-01 15:36:13.078');