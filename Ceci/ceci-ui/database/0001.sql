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

ALTER TABLE public.bus OWNER TO ceci;

CREATE TABLE element (
    elementid character varying(255) NOT NULL,
    category character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    treedepth integer NOT NULL,
    treeindex integer NOT NULL,
    type integer NOT NULL,
    owner_companyid character varying(255) NOT NULL,
    parentid character varying(255) NOT NULL
);

ALTER TABLE public.element OWNER TO ceci;

CREATE TABLE event (
    eventid character varying(255) NOT NULL,
    content character varying(1024) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    processed timestamp without time zone,
    owner_companyid character varying(255) NOT NULL,
    processingerror boolean
);

ALTER TABLE public.event OWNER TO ceci;

CREATE TABLE record (
    recordid character varying(255) NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    value numeric(38,2) NOT NULL,
    owner_companyid character varying(255) NOT NULL,
    recordset_recordsetid character varying(255) NOT NULL
);

ALTER TABLE public.record OWNER TO ceci;

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

ALTER TABLE public.recordset OWNER TO ceci;

ALTER TABLE ONLY bus
    ADD CONSTRAINT bus_pkey PRIMARY KEY (busid);

ALTER TABLE ONLY element
    ADD CONSTRAINT element_pkey PRIMARY KEY (elementid);

ALTER TABLE ONLY event
    ADD CONSTRAINT event_pkey PRIMARY KEY (eventid);

ALTER TABLE ONLY record
    ADD CONSTRAINT record_pkey PRIMARY KEY (recordid);

ALTER TABLE ONLY recordset
    ADD CONSTRAINT recordset_pkey PRIMARY KEY (recordsetid);

ALTER TABLE ONLY bus
    ADD CONSTRAINT fk_bus_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

ALTER TABLE ONLY element
    ADD CONSTRAINT fk_element_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

ALTER TABLE ONLY event
    ADD CONSTRAINT fk_event_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

ALTER TABLE ONLY record
    ADD CONSTRAINT fk_record_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

ALTER TABLE ONLY record
    ADD CONSTRAINT fk_record_recordset_recordsetid FOREIGN KEY (recordset_recordsetid) REFERENCES recordset(recordsetid);

ALTER TABLE ONLY recordset
    ADD CONSTRAINT fk_recordset_element_elementid FOREIGN KEY (element_elementid) REFERENCES element(elementid);

ALTER TABLE ONLY recordset
    ADD CONSTRAINT fk_recordset_owner_companyid FOREIGN KEY (owner_companyid) REFERENCES company(companyid);

INSERT INTO bus VALUES ('C9236CCE-EE45-4FBB-BB3C-6CB495755677', '2013-04-03 10:05:21.788', '2013-04-10 22:49:37.921', 'Local Bus', '3248528E-4D90-41F7-968F-AF255AD16901', 1, '2013-04-10 22:49:37.921', 'localhost', 5672, 'admin', 'admin');

ALTER TABLE element
   ALTER COLUMN parent_elementid DROP NOT NULL;
UPDATE element SET parent_elementid = NULL;

ALTER TABLE bus OWNER TO ceci;
ALTER TABLE company OWNER TO ceci;
ALTER TABLE customer OWNER TO ceci;
ALTER TABLE element OWNER TO ceci;
ALTER TABLE event OWNER TO ceci;
ALTER TABLE group_ OWNER TO ceci;
ALTER TABLE groupmember OWNER TO ceci;
ALTER TABLE postaladdress OWNER TO ceci;
ALTER TABLE privilege OWNER TO ceci;
ALTER TABLE record OWNER TO ceci;
ALTER TABLE recordset OWNER TO ceci;
ALTER TABLE schemaversion OWNER TO ceci;
ALTER TABLE user_ OWNER TO ceci;

INSERT INTO schemaversion VALUES (NOW(), 'ceci', '0001');
