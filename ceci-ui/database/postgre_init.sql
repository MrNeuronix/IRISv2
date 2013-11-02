--
-- Name: bus; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE bus (
    busid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    owner_companyid character varying(255) NOT NULL,
    connectionstatus integer NOT NULL,
    inventorysynchronized timestamp without time zone NOT NULL,
    host character varying(255),
    port integer,
    username character varying(255),
    userpassword character varying(255)
);


--
-- Name: company; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE company (
    companyid character varying(255) NOT NULL,
    phonenumber character varying(255) NOT NULL,
    invoicingemailaddress character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    salesemailaddress character varying(255) NOT NULL,
    companyname character varying(255) NOT NULL,
    supportemailaddress character varying(255) NOT NULL,
    companycode character varying(255) NOT NULL,
    modified timestamp without time zone NOT NULL,
    deliveryaddress_postaladdressid character varying(255),
    invoicingaddress_postaladdressid character varying(255),
    iban character varying(255) NOT NULL,
    bic character varying(255) NOT NULL,
    host character varying(255),
    termsandconditions character varying(4096),
    url character varying(255)
);


--
-- Name: customer; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE customer (
    customerid character varying(255) NOT NULL,
    lastname character varying(255) NOT NULL,
    phonenumber character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    company boolean NOT NULL,
    emailaddress character varying(255) NOT NULL,
    firstname character varying(255) NOT NULL,
    companyname character varying(255),
    modified timestamp without time zone NOT NULL,
    companycode character varying(255),
    deliveryaddress_postaladdressid character varying(255),
    invoicingaddress_postaladdressid character varying(255),
    owner_companyid character varying(255) NOT NULL,
    admingroup_groupid character varying(255),
    membergroup_groupid character varying(255)
);


--
-- Name: element; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE element (
    elementid character varying(255) NOT NULL,
    category character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    treedepth integer NOT NULL,
    treeindex integer NOT NULL,
    type integer NOT NULL,
    bus_busid character varying(255),
    owner_companyid character varying(255) NOT NULL,
    parent_elementid character varying(255)
);


--
-- Name: event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE event (
    eventid character varying(255) NOT NULL,
    content character varying(1024) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    processed timestamp without time zone,
    owner_companyid character varying(255) NOT NULL,
    processingerror boolean
);


--
-- Name: group_; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE group_ (
    groupid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    modified timestamp without time zone NOT NULL,
    owner_companyid character varying(255) NOT NULL
);


--
-- Name: groupmember; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE groupmember (
    groupmemberid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    group_groupid character varying(255),
    user_userid character varying(255)
);


--
-- Name: postaladdress; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE postaladdress (
    postaladdressid character varying(255) NOT NULL,
    addresslinetwo character varying(255),
    addresslinethree character varying(255),
    postalcode character varying(255),
    addresslineone character varying(255),
    city character varying(255),
    country character varying(255)
);


--
-- Name: privilege; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE privilege (
    privilegeid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    dataid character varying(255),
    key character varying(255) NOT NULL,
    user_userid character varying(255),
    group_groupid character varying(255)
);


--
-- Name: record; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE record (
    recordid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    value numeric(38,2) NOT NULL,
    owner_companyid character varying(255) NOT NULL,
    recordset_recordsetid character varying(255) NOT NULL
);


--
-- Name: recordset; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE recordset (
    recordsetid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    type integer NOT NULL,
    unit character varying(255) NOT NULL,
    element_elementid character varying(255) NOT NULL,
    owner_companyid character varying(255) NOT NULL
);


--
-- Name: schemaversion; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE schemaversion (
    created timestamp without time zone NOT NULL,
    schemaname character varying(255) NOT NULL,
    schemaversion character varying(255) NOT NULL
);


--
-- Name: user_; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE user_ (
    userid character varying(255) NOT NULL,
    lastname character varying(255) NOT NULL,
    phonenumber character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    emailaddress character varying(255) NOT NULL,
    firstname character varying(255) NOT NULL,
    modified timestamp without time zone NOT NULL,
    owner_companyid character varying(255) NOT NULL,
    passwordhash character varying(256) NOT NULL,
    emailaddressvalidated boolean NOT NULL
);


INSERT INTO schemaversion (created, schemaname, schemaversion) VALUES ('2013-11-02 09:48:13.067602', 'ceci', '0000');
INSERT INTO postaladdress (postaladdressid, addresslinetwo, addresslinethree, postalcode, addresslineone, city, country) VALUES ('CFE997C0-3FAF-4F6C-BBED-DB09689936B6', '-', '-', '00000', '-', 'Helsinki', 'Finland');
INSERT INTO postaladdress (postaladdressid, addresslinetwo, addresslinethree, postalcode, addresslineone, city, country) VALUES ('4EA7E643-3C80-49B2-8D1C-AAFA7E66A28C', '-', '-', '00000', '-', 'Helsinki', 'Finland');
INSERT INTO company (companyid, phonenumber, invoicingemailaddress, created, salesemailaddress, companyname, supportemailaddress, companycode, modified, deliveryaddress_postaladdressid, invoicingaddress_postaladdressid, iban, bic, host, termsandconditions, url) VALUES ('3248528E-4D90-41F7-968F-AF255AD16901', '+358 40 1639099', 'invoice@bare.com', '2011-04-22 08:52:13.899', 'sales@bare.com', 'Test Company', 'support@bare.com', '0000000-0', '2011-04-22 08:52:13.899', '4EA7E643-3C80-49B2-8D1C-AAFA7E66A28C', 'CFE997C0-3FAF-4F6C-BBED-DB09689936B6', '-', '-', '127.0.0.1', '-', 'http://127.0.0.1:8085/ceci');
INSERT INTO group_ (groupid, created, description, name, modified, owner_companyid) VALUES ('3DE5D850-B015-44C1-904C-86DC2B0276A4', '2012-02-13 21:37:24.804', 'Users', 'user', '2012-02-13 21:37:24.804', '3248528E-4D90-41F7-968F-AF255AD16901');
INSERT INTO group_ (groupid, created, description, name, modified, owner_companyid) VALUES ('1DE5D850-B015-44C1-904C-86DC2B0276A3', '2012-06-25 19:57:00', 'Administrators', 'administrator', '2012-06-25 19:57:00', '3248528E-4D90-41F7-968F-AF255AD16901');
INSERT INTO user_ (userid, lastname, phonenumber, created, emailaddress, firstname, modified, owner_companyid, passwordhash, emailaddressvalidated) VALUES ('A591FCB8-772E-4157-B64B-4371A6C7CAE0', 'Test', '+123', '2013-03-29 18:21:23.769', 'admin@admin.org', 'User', '2013-03-29 18:21:23.769', '3248528E-4D90-41F7-968F-AF255AD16901', 'c8213c753f70e6ef82a3bbece671c183cc9aa70d944f2d8abbbc50ab7432f2b4', true);
INSERT INTO groupmember (groupmemberid, created, group_groupid, user_userid) VALUES ('50413BBB-DB86-402E-9E98-C7E73F219827', '2013-03-29 19:11:42.986', '1DE5D850-B015-44C1-904C-86DC2B0276A3', 'A591FCB8-772E-4157-B64B-4371A6C7CAE0');
INSERT INTO bus (busid, created, modified, name, owner_companyid, connectionstatus, inventorysynchronized, host, port, username, userpassword) VALUES ('C9236CCE-EE45-4FBB-BB3C-6CB495755677', '2013-04-03 10:05:21.788', '2013-11-02 10:11:29.718', 'Local Bus', '3248528E-4D90-41F7-968F-AF255AD16901', 3, '2013-04-10 22:49:37.921', 'localhost', 5672, 'admin', 'admin');


--
-- Name: bus_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE bus
    ADD CONSTRAINT bus_pkey PRIMARY KEY (busid);


--
-- Name: company_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE company
    ADD CONSTRAINT company_pkey PRIMARY KEY (companyid);


--
-- Name: customer_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (customerid);


--
-- Name: element_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE element
    ADD CONSTRAINT element_pkey PRIMARY KEY (elementid);


--
-- Name: event_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE event
    ADD CONSTRAINT event_pkey PRIMARY KEY (eventid);


--
-- Name: group__pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE group_
    ADD CONSTRAINT group__pkey PRIMARY KEY (groupid);


--
-- Name: groupmember_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE groupmember
    ADD CONSTRAINT groupmember_pkey PRIMARY KEY (groupmemberid);


--
-- Name: postaladdress_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE postaladdress
    ADD CONSTRAINT postaladdress_pkey PRIMARY KEY (postaladdressid);


--
-- Name: privilege_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE privilege
    ADD CONSTRAINT privilege_pkey PRIMARY KEY (privilegeid);


--
-- Name: record_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE record
    ADD CONSTRAINT record_pkey PRIMARY KEY (recordid);


--
-- Name: recordset_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE recordset
    ADD CONSTRAINT recordset_pkey PRIMARY KEY (recordsetid);


--
-- Name: schemaversion_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE schemaversion
    ADD CONSTRAINT schemaversion_pkey PRIMARY KEY (created);


--
-- Name: unq_group__0; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE group_
    ADD CONSTRAINT unq_group__0 UNIQUE (owner_companyid, name);


--
-- Name: unq_groupmember_0; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE groupmember
    ADD CONSTRAINT unq_groupmember_0 UNIQUE (user_userid, group_groupid);


--
-- Name: unq_privilege_0; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE privilege
    ADD CONSTRAINT unq_privilege_0 UNIQUE (user_userid, group_groupid);


--
-- Name: unq_user__0; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE user_
    ADD CONSTRAINT unq_user__0 UNIQUE (owner_companyid, emailaddress);


--
-- Name: user__pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE user_
    ADD CONSTRAINT user__pkey PRIMARY KEY (userid);


--
-- Name: fk_bus_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE bus
    ADD CONSTRAINT fk_bus_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_company_deliveryaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE company
    ADD CONSTRAINT fk_company_deliveryaddress_postaladdressid FOREIGN KEY (deliveryaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- Name: fk_company_invoicingaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE company
    ADD CONSTRAINT fk_company_invoicingaddress_postaladdressid FOREIGN KEY (invoicingaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- Name: fk_customer_admingroup_groupid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_admingroup_groupid FOREIGN KEY (admingroup_groupid) REFERENCES group_(groupid);


--
-- Name: fk_customer_billingaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_billingaddress_postaladdressid FOREIGN KEY (invoicingaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- Name: fk_customer_deliveryaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_deliveryaddress_postaladdressid FOREIGN KEY (deliveryaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- Name: fk_customer_membergroup_groupid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_membergroup_groupid FOREIGN KEY (membergroup_groupid) REFERENCES group_(groupid);


--
-- Name: fk_customer_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_element_bus_busid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE element
    ADD CONSTRAINT fk_element_bus_busid FOREIGN KEY (bus_busid) REFERENCES bus(busid);


--
-- Name: fk_element_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE element
    ADD CONSTRAINT fk_element_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_element_parent_elementid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE element
    ADD CONSTRAINT fk_element_parent_elementid FOREIGN KEY (parent_elementid) REFERENCES element(elementid);


--
-- Name: fk_event_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE event
    ADD CONSTRAINT fk_event_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_group__owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE group_
    ADD CONSTRAINT fk_group__owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_groupmember_group_groupid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE groupmember
    ADD CONSTRAINT fk_groupmember_group_groupid FOREIGN KEY (group_groupid) REFERENCES group_(groupid);


--
-- Name: fk_groupmember_user_userid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE groupmember
    ADD CONSTRAINT fk_groupmember_user_userid FOREIGN KEY (user_userid) REFERENCES user_(userid);


--
-- Name: fk_privilege_group_groupid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE privilege
    ADD CONSTRAINT fk_privilege_group_groupid FOREIGN KEY (group_groupid) REFERENCES group_(groupid);


--
-- Name: fk_privilege_user_userid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE privilege
    ADD CONSTRAINT fk_privilege_user_userid FOREIGN KEY (user_userid) REFERENCES user_(userid);


--
-- Name: fk_record_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE record
    ADD CONSTRAINT fk_record_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_record_recordset_recordsetid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE record
    ADD CONSTRAINT fk_record_recordset_recordsetid FOREIGN KEY (recordset_recordsetid) REFERENCES recordset(recordsetid);


--
-- Name: fk_recordset_element_elementid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE recordset
    ADD CONSTRAINT fk_recordset_element_elementid FOREIGN KEY (element_elementid) REFERENCES element(elementid);


--
-- Name: fk_recordset_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE recordset
    ADD CONSTRAINT fk_recordset_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- Name: fk_user__owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE user_
    ADD CONSTRAINT fk_user__owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);
