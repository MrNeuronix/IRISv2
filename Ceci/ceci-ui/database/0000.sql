CREATE USER ceci WITH PASSWORD 'ceci';
CREATE DATABASE ceci WITH OWNER ceci;

\connect ceci

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
    termsandconditions character varying(4096)
);


ALTER TABLE public.company OWNER TO ceci;

--
-- TOC entry 162 (class 1259 OID 16696)
-- Dependencies: 6
-- Name: customer; Type: TABLE; Schema: public; Owner: ceci; Tablespace:
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
    owner_companyid character varying(255) NOT NULL
);


ALTER TABLE public.customer OWNER TO ceci;

--
-- TOC entry 163 (class 1259 OID 16702)
-- Dependencies: 6
-- Name: group_; Type: TABLE; Schema: public; Owner: ceci; Tablespace:
--

CREATE TABLE group_ (
    groupid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    modified timestamp without time zone NOT NULL,
    owner_companyid character varying(255) NOT NULL
);


ALTER TABLE public.group_ OWNER TO ceci;

--
-- TOC entry 164 (class 1259 OID 16708)
-- Dependencies: 6
-- Name: groupmember; Type: TABLE; Schema: public; Owner: ceci; Tablespace:
--

CREATE TABLE groupmember (
    groupmemberid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    group_groupid character varying(255),
    user_userid character varying(255)
);


ALTER TABLE public.groupmember OWNER TO ceci;

--
-- TOC entry 165 (class 1259 OID 16750)
-- Dependencies: 6
-- Name: postaladdress; Type: TABLE; Schema: public; Owner: ceci; Tablespace:
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


ALTER TABLE public.postaladdress OWNER TO ceci;

--
-- TOC entry 167 (class 1259 OID 16952)
-- Dependencies: 6
-- Name: privilege; Type: TABLE; Schema: public; Owner: ceci; Tablespace:
--

CREATE TABLE privilege (
    privilegeid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    dataid character varying(255),
    key character varying(255) NOT NULL,
    user_userid character varying(255),
    group_groupid character varying(255)
);


ALTER TABLE public.privilege OWNER TO ceci;

--
-- TOC entry 166 (class 1259 OID 16774)
-- Dependencies: 6
-- Name: user_; Type: TABLE; Schema: public; Owner: ceci; Tablespace:
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
    passwordhash character varying(256) NOT NULL
);


ALTER TABLE public.user_ OWNER TO ceci;

--
-- TOC entry 1914 (class 0 OID 16690)
-- Dependencies: 161 1921
-- Data for Name: company; Type: TABLE DATA; Schema: public; Owner: ceci
--

INSERT INTO company VALUES ('3248528E-4D90-41F7-968F-AF255AD16901', '+358 40 1639099', 'invoice@bare.com', '2011-04-22 08:52:13.899', 'sales@bare.com', 'Test Company', 'support@bare.com', '0000000-0', '2011-04-22 08:52:13.899', '4EA7E643-3C80-49B2-8D1C-AAFA7E66A28C', 'CFE997C0-3FAF-4F6C-BBED-DB09689936B6', '-', '-', '127.0.0.1', '-');


--
-- TOC entry 1915 (class 0 OID 16696)
-- Dependencies: 162 1921
-- Data for Name: customer; Type: TABLE DATA; Schema: public; Owner: ceci
--



--
-- TOC entry 1916 (class 0 OID 16702)
-- Dependencies: 163 1921
-- Data for Name: group_; Type: TABLE DATA; Schema: public; Owner: ceci
--

INSERT INTO group_ VALUES ('3DE5D850-B015-44C1-904C-86DC2B0276A4', '2012-02-13 21:37:24.804', 'Users', 'user', '2012-02-13 21:37:24.804', '3248528E-4D90-41F7-968F-AF255AD16901');
INSERT INTO group_ VALUES ('1DE5D850-B015-44C1-904C-86DC2B0276A3', '2012-06-25 19:57:00', 'Administrators', 'administrator', '2012-06-25 19:57:00', '3248528E-4D90-41F7-968F-AF255AD16901');


--
-- TOC entry 1917 (class 0 OID 16708)
-- Dependencies: 164 1921
-- Data for Name: groupmember; Type: TABLE DATA; Schema: public; Owner: ceci
--

INSERT INTO groupmember VALUES ('50413BBB-DB86-402E-9E98-C7E73F219827', '2013-03-29 19:11:42.986', '1DE5D850-B015-44C1-904C-86DC2B0276A3', 'A591FCB8-772E-4157-B64B-4371A6C7CAE0');


--
-- TOC entry 1918 (class 0 OID 16750)
-- Dependencies: 165 1921
-- Data for Name: postaladdress; Type: TABLE DATA; Schema: public; Owner: ceci
--

INSERT INTO postaladdress VALUES ('CFE997C0-3FAF-4F6C-BBED-DB09689936B6', '-', '-', '00000', '-', 'Helsinki', 'Finland');
INSERT INTO postaladdress VALUES ('4EA7E643-3C80-49B2-8D1C-AAFA7E66A28C', '-', '-', '00000', '-', 'Helsinki', 'Finland');


--
-- TOC entry 1920 (class 0 OID 16952)
-- Dependencies: 167 1921
-- Data for Name: privilege; Type: TABLE DATA; Schema: public; Owner: ceci
--



--
-- TOC entry 1919 (class 0 OID 16774)
-- Dependencies: 166 1921
-- Data for Name: user_; Type: TABLE DATA; Schema: public; Owner: ceci
--

INSERT INTO user_ VALUES ('A591FCB8-772E-4157-B64B-4371A6C7CAE0', 'Test', '+123', '2013-03-29 18:21:23.769', 'admin@admin.org', 'User', '2013-03-29 18:21:23.769', '3248528E-4D90-41F7-968F-AF255AD16901', 'c8213c753f70e6ef82a3bbece671c183cc9aa70d944f2d8abbbc50ab7432f2b4');


--
-- TOC entry 1882 (class 2606 OID 16788)
-- Dependencies: 161 161 1922
-- Name: company_pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY company
    ADD CONSTRAINT company_pkey PRIMARY KEY (companyid);


--
-- TOC entry 1884 (class 2606 OID 16790)
-- Dependencies: 162 162 1922
-- Name: customer_pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (customerid);


--
-- TOC entry 1886 (class 2606 OID 16792)
-- Dependencies: 163 163 1922
-- Name: group__pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY group_
    ADD CONSTRAINT group__pkey PRIMARY KEY (groupid);


--
-- TOC entry 1890 (class 2606 OID 16794)
-- Dependencies: 164 164 1922
-- Name: groupmember_pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY groupmember
    ADD CONSTRAINT groupmember_pkey PRIMARY KEY (groupmemberid);


--
-- TOC entry 1894 (class 2606 OID 16802)
-- Dependencies: 165 165 1922
-- Name: postaladdress_pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY postaladdress
    ADD CONSTRAINT postaladdress_pkey PRIMARY KEY (postaladdressid);


--
-- TOC entry 1900 (class 2606 OID 16959)
-- Dependencies: 167 167 1922
-- Name: privilege_pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY privilege
    ADD CONSTRAINT privilege_pkey PRIMARY KEY (privilegeid);


--
-- TOC entry 1888 (class 2606 OID 16810)
-- Dependencies: 163 163 163 1922
-- Name: unq_group__0; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY group_
    ADD CONSTRAINT unq_group__0 UNIQUE (owner_companyid, name);


--
-- TOC entry 1892 (class 2606 OID 16812)
-- Dependencies: 164 164 164 1922
-- Name: unq_groupmember_0; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY groupmember
    ADD CONSTRAINT unq_groupmember_0 UNIQUE (user_userid, group_groupid);


--
-- TOC entry 1902 (class 2606 OID 16961)
-- Dependencies: 167 167 167 1922
-- Name: unq_privilege_0; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY privilege
    ADD CONSTRAINT unq_privilege_0 UNIQUE (user_userid, group_groupid);


--
-- TOC entry 1896 (class 2606 OID 16818)
-- Dependencies: 166 166 166 1922
-- Name: unq_user__0; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY user_
    ADD CONSTRAINT unq_user__0 UNIQUE (owner_companyid, emailaddress);


--
-- TOC entry 1898 (class 2606 OID 16820)
-- Dependencies: 166 166 1922
-- Name: user__pkey; Type: CONSTRAINT; Schema: public; Owner: ceci; Tablespace:
--

ALTER TABLE ONLY user_
    ADD CONSTRAINT user__pkey PRIMARY KEY (userid);


--
-- TOC entry 1903 (class 2606 OID 16841)
-- Dependencies: 165 1893 161 1922
-- Name: fk_company_deliveryaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY company
    ADD CONSTRAINT fk_company_deliveryaddress_postaladdressid FOREIGN KEY (deliveryaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- TOC entry 1904 (class 2606 OID 16846)
-- Dependencies: 1893 161 165 1922
-- Name: fk_company_invoicingaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY company
    ADD CONSTRAINT fk_company_invoicingaddress_postaladdressid FOREIGN KEY (invoicingaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- TOC entry 1905 (class 2606 OID 16851)
-- Dependencies: 165 162 1893 1922
-- Name: fk_customer_billingaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY customer
    ADD CONSTRAINT fk_customer_billingaddress_postaladdressid FOREIGN KEY (invoicingaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- TOC entry 1906 (class 2606 OID 16856)
-- Dependencies: 162 165 1893 1922
-- Name: fk_customer_deliveryaddress_postaladdressid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY customer
    ADD CONSTRAINT fk_customer_deliveryaddress_postaladdressid FOREIGN KEY (deliveryaddress_postaladdressid) REFERENCES postaladdress(postaladdressid);


--
-- TOC entry 1907 (class 2606 OID 16861)
-- Dependencies: 161 162 1881 1922
-- Name: fk_customer_owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY customer
    ADD CONSTRAINT fk_customer_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- TOC entry 1908 (class 2606 OID 16866)
-- Dependencies: 1881 163 161 1922
-- Name: fk_group__owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY group_
    ADD CONSTRAINT fk_group__owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);


--
-- TOC entry 1909 (class 2606 OID 16871)
-- Dependencies: 1885 163 164 1922
-- Name: fk_groupmember_group_groupid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY groupmember
    ADD CONSTRAINT fk_groupmember_group_groupid FOREIGN KEY (group_groupid) REFERENCES group_(groupid);


--
-- TOC entry 1910 (class 2606 OID 16876)
-- Dependencies: 166 1897 164 1922
-- Name: fk_groupmember_user_userid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY groupmember
    ADD CONSTRAINT fk_groupmember_user_userid FOREIGN KEY (user_userid) REFERENCES user_(userid);


--
-- TOC entry 1912 (class 2606 OID 16962)
-- Dependencies: 1885 163 167 1922
-- Name: fk_privilege_group_groupid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY privilege
    ADD CONSTRAINT fk_privilege_group_groupid FOREIGN KEY (group_groupid) REFERENCES group_(groupid);


--
-- TOC entry 1913 (class 2606 OID 16967)
-- Dependencies: 166 167 1897 1922
-- Name: fk_privilege_user_userid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY privilege
    ADD CONSTRAINT fk_privilege_user_userid FOREIGN KEY (user_userid) REFERENCES user_(userid);


--
-- TOC entry 1911 (class 2606 OID 16941)
-- Dependencies: 161 166 1881 1922
-- Name: fk_user__owner_companyid; Type: FK CONSTRAINT; Schema: public; Owner: ceci
--

ALTER TABLE ONLY user_
    ADD CONSTRAINT fk_user__owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

ALTER TABLE user_ ADD EMAILADDRESSVALIDATED BOOLEAN;
UPDATE user_ SET EMAILADDRESSVALIDATED = true;
ALTER TABLE user_ ALTER COLUMN emailaddressvalidated SET NOT NULL;

ALTER TABLE customer ADD COLUMN admingroup_groupid character varying(255);
ALTER TABLE customer ADD COLUMN membergroup_groupid character varying(255);

ALTER TABLE customer
  ADD CONSTRAINT fk_customer_admingroup_groupid FOREIGN KEY (admingroup_groupid)
      REFERENCES group_ (groupid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE customer
  ADD CONSTRAINT fk_customer_membergroup_groupid FOREIGN KEY (membergroup_groupid)
      REFERENCES group_ (groupid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE company ADD COLUMN url character varying(255);
  UPDATE COMPANY SET url = 'http://127.0.0.1:8085/ceci';

  CREATE TABLE schemaversion
  (
    created timestamp without time zone NOT NULL,
    schemaname character varying(255) NOT NULL,
    schemaversion character varying(255) NOT NULL,
    CONSTRAINT schemaversion_pkey PRIMARY KEY (created )
  )
  WITH (
    OIDS=FALSE
  );
  ALTER TABLE schemaversion
    OWNER TO ceci;

  INSERT INTO schemaversion VALUES (NOW(), 'ceci', '0000');
