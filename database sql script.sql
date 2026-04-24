--
-- PostgreSQL database dump
--

\restrict YuDS5YEYwu3EOlLHYc4UMPfjdmffYl05d5fyJsgF1ow4T8vUEkWTL7qIHgg5HAY

-- Dumped from database version 16.13
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: julienjeanpierre
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO julienjeanpierre;

--
-- Name: log_requests_insert(); Type: FUNCTION; Schema: public; Owner: julienjeanpierre
--

CREATE FUNCTION public.log_requests_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        -- Trigger logic: log the insert operation
        INSERT INTO audit_trail (action_type, entity_type, entity_id, action_description, timestamp)
        VALUES ('INSERT', 'REQUESTS', NEW.id::text, 'New request inserted with status: ' || NEW.status, CURRENT_TIMESTAMP);
        RETURN NEW;
    END;
    $$;


ALTER FUNCTION public.log_requests_insert() OWNER TO julienjeanpierre;

--
-- Name: normalize_redemption_audit_before_insert(); Type: FUNCTION; Schema: public; Owner: julienjeanpierre
--

CREATE FUNCTION public.normalize_redemption_audit_before_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        NEW.redemption_time := COALESCE(NEW.redemption_time, NOW());
        NEW.status := UPPER(COALESCE(NEW.status, 'FAILED'));
        IF NEW.status NOT IN ('SUCCESS', 'FAILED') THEN
            NEW.status := 'FAILED';
        END IF;
        NEW.branch := COALESCE(NULLIF(NEW.branch, ''), 'Unknown Company');
        RETURN NEW;
    END;
    $$;


ALTER FUNCTION public.normalize_redemption_audit_before_insert() OWNER TO julienjeanpierre;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_trail; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.audit_trail (
    audit_id integer NOT NULL,
    action_type character varying(100) NOT NULL,
    entity_type character varying(100) NOT NULL,
    entity_id character varying(255),
    user_name character varying(100),
    action_description text,
    old_value text,
    new_value text,
    "timestamp" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ip_address character varying(50),
    context text
);


ALTER TABLE public.audit_trail OWNER TO julienjeanpierre;

--
-- Name: audit_trail_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.audit_trail_audit_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.audit_trail_audit_id_seq OWNER TO julienjeanpierre;

--
-- Name: audit_trail_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: julienjeanpierre
--

ALTER SEQUENCE public.audit_trail_audit_id_seq OWNED BY public.audit_trail.audit_id;


--
-- Name: branch_branch_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.branch_branch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.branch_branch_id_seq OWNER TO julienjeanpierre;

--
-- Name: branch; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.branch (
    branch_id integer DEFAULT nextval('public.branch_branch_id_seq'::regclass) NOT NULL,
    location character varying(255) NOT NULL,
    responsible_user character varying(50) NOT NULL,
    ref_company integer,
    address_branch character varying(255),
    phone_branch character varying(50),
    company character varying(255),
    phone character varying(20),
    industry character varying(255)
);


ALTER TABLE public.branch OWNER TO julienjeanpierre;

--
-- Name: clients_ref_client_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.clients_ref_client_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.clients_ref_client_seq OWNER TO julienjeanpierre;

--
-- Name: clients; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.clients (
    ref_client integer DEFAULT nextval('public.clients_ref_client_seq'::regclass) NOT NULL,
    nom_client character varying(100) NOT NULL,
    email_client character varying(100) NOT NULL,
    address_client character varying(255),
    phone_client character varying(30)
);


ALTER TABLE public.clients OWNER TO julienjeanpierre;

--
-- Name: company_company_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.company_company_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.company_company_id_seq OWNER TO julienjeanpierre;

--
-- Name: company; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.company (
    company_id integer DEFAULT nextval('public.company_company_id_seq'::regclass) NOT NULL,
    name_company character varying(255) NOT NULL,
    email_company character varying(255),
    industry_type character varying(100)
);


ALTER TABLE public.company OWNER TO julienjeanpierre;

--
-- Name: invoices; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.invoices (
    invoice_id integer NOT NULL,
    invoice_number character varying(50) NOT NULL,
    request_id integer NOT NULL,
    request_reference character varying(50) NOT NULL,
    ref_client integer NOT NULL,
    client_name character varying(255) NOT NULL,
    total_amount numeric(10,2) NOT NULL,
    status character varying(50) DEFAULT 'pending'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.invoices OWNER TO julienjeanpierre;

--
-- Name: invoices_invoice_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.invoices_invoice_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.invoices_invoice_id_seq OWNER TO julienjeanpierre;

--
-- Name: invoices_invoice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: julienjeanpierre
--

ALTER SEQUENCE public.invoices_invoice_id_seq OWNED BY public.invoices.invoice_id;


--
-- Name: redemption_audit; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.redemption_audit (
    redemption_id integer NOT NULL,
    redemption_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status character varying(20) NOT NULL,
    branch character varying(255) NOT NULL,
    voucher_code character varying(100),
    redeemed_by character varying(100),
    message text
);


ALTER TABLE public.redemption_audit OWNER TO julienjeanpierre;

--
-- Name: redemption_audit_redemption_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.redemption_audit_redemption_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.redemption_audit_redemption_id_seq OWNER TO julienjeanpierre;

--
-- Name: redemption_audit_redemption_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: julienjeanpierre
--

ALTER SEQUENCE public.redemption_audit_redemption_id_seq OWNED BY public.redemption_audit.redemption_id;


--
-- Name: redemptions; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.redemptions (
    redemption_id integer NOT NULL,
    voucher_code character varying(100) NOT NULL,
    branch_id integer,
    branch_location character varying(255),
    redeemed_by character varying(100),
    redemption_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    redemption_time time without time zone DEFAULT CURRENT_TIME,
    status character varying(50) DEFAULT 'completed'::character varying,
    notes text
);


ALTER TABLE public.redemptions OWNER TO julienjeanpierre;

--
-- Name: redemptions_redemption_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.redemptions_redemption_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.redemptions_redemption_id_seq OWNER TO julienjeanpierre;

--
-- Name: redemptions_redemption_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: julienjeanpierre
--

ALTER SEQUENCE public.redemptions_redemption_id_seq OWNED BY public.redemptions.redemption_id;


--
-- Name: requests_ref_request_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.requests_ref_request_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.requests_ref_request_seq OWNER TO julienjeanpierre;

--
-- Name: requests; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.requests (
    ref_request integer DEFAULT nextval('public.requests_ref_request_seq'::regclass) NOT NULL,
    creation_date date NOT NULL,
    num_voucher integer,
    status character varying(50),
    payment character varying(50),
    date_payment date,
    ref_payment integer,
    date_approval date,
    duration_voucher integer,
    ref_client integer NOT NULL,
    processed_by character varying(50),
    approved_by character varying(50),
    validated_by character varying(50),
    request_reference character varying(50),
    client_name character varying(255),
    unit_value numeric(10,2),
    total_value numeric(10,2),
    payment_status character varying(50) DEFAULT 'unpaid'::character varying,
    approver_email character varying(255),
    invoice_id integer,
    vouchers_generated boolean DEFAULT false,
    vouchers_sent boolean DEFAULT false
);


ALTER TABLE public.requests OWNER TO julienjeanpierre;

--
-- Name: users; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.users (
    username character varying(50) NOT NULL,
    first_name_user character varying(50) NOT NULL,
    last_name_user character varying(50) NOT NULL,
    email_user character varying(100) NOT NULL,
    role character varying(50),
    password character varying(255) NOT NULL,
    ddl character varying(100),
    titre character varying(100),
    status character varying(50),
    company character varying(255)
);


ALTER TABLE public.users OWNER TO julienjeanpierre;

--
-- Name: users_username_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.users_username_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_username_seq OWNER TO julienjeanpierre;

--
-- Name: voucher_requests; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.voucher_requests (
    request_id integer NOT NULL,
    request_reference character varying(50) NOT NULL,
    ref_client integer NOT NULL,
    client_name character varying(255) NOT NULL,
    num_vouchers integer NOT NULL,
    unit_value numeric(10,2) NOT NULL,
    total_value numeric(10,2) NOT NULL,
    status character varying(50) DEFAULT 'initiated'::character varying,
    payment_status character varying(50) DEFAULT 'unpaid'::character varying,
    payment_date timestamp without time zone,
    approved_by character varying(100),
    approval_date timestamp without time zone,
    processed_by character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    client_email character varying(255),
    vouchers_generated boolean DEFAULT false,
    vouchers_sent boolean DEFAULT false,
    expiration_date date
);


ALTER TABLE public.voucher_requests OWNER TO julienjeanpierre;

--
-- Name: voucher_requests_request_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.voucher_requests_request_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.voucher_requests_request_id_seq OWNER TO julienjeanpierre;

--
-- Name: voucher_requests_request_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: julienjeanpierre
--

ALTER SEQUENCE public.voucher_requests_request_id_seq OWNED BY public.voucher_requests.request_id;


--
-- Name: voucher_stores; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.voucher_stores (
    voucher_store_id integer NOT NULL,
    voucher_code character varying(100) NOT NULL,
    branch_id integer,
    company character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.voucher_stores OWNER TO julienjeanpierre;

--
-- Name: voucher_stores_voucher_store_id_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.voucher_stores_voucher_store_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.voucher_stores_voucher_store_id_seq OWNER TO julienjeanpierre;

--
-- Name: voucher_stores_voucher_store_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: julienjeanpierre
--

ALTER SEQUENCE public.voucher_stores_voucher_store_id_seq OWNED BY public.voucher_stores.voucher_store_id;


--
-- Name: vouchers_ref_voucher_seq; Type: SEQUENCE; Schema: public; Owner: julienjeanpierre
--

CREATE SEQUENCE public.vouchers_ref_voucher_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.vouchers_ref_voucher_seq OWNER TO julienjeanpierre;

--
-- Name: vouchers; Type: TABLE; Schema: public; Owner: julienjeanpierre
--

CREATE TABLE public.vouchers (
    ref_voucher character varying(100) DEFAULT nextval('public.vouchers_ref_voucher_seq'::regclass) NOT NULL,
    val_voucher character varying NOT NULL,
    init_date date,
    expiry_date date,
    status_voucher character varying(50),
    date_redeemed date,
    bearer character varying(100),
    ref_request integer,
    redeemed_by character varying(50),
    redeemed_branch character varying(255),
    ref_client integer,
    redeemed boolean DEFAULT false,
    code_voucher character varying(100),
    price numeric(10,2),
    request_id integer,
    request_reference character varying(50),
    assigned_to_request boolean DEFAULT false,
    qr_code_data text,
    pdf_path character varying(500),
    email_sent boolean DEFAULT false,
    email_sent_date timestamp without time zone
);


ALTER TABLE public.vouchers OWNER TO julienjeanpierre;

--
-- Name: audit_trail audit_id; Type: DEFAULT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.audit_trail ALTER COLUMN audit_id SET DEFAULT nextval('public.audit_trail_audit_id_seq'::regclass);


--
-- Name: invoices invoice_id; Type: DEFAULT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.invoices ALTER COLUMN invoice_id SET DEFAULT nextval('public.invoices_invoice_id_seq'::regclass);


--
-- Name: redemption_audit redemption_id; Type: DEFAULT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.redemption_audit ALTER COLUMN redemption_id SET DEFAULT nextval('public.redemption_audit_redemption_id_seq'::regclass);


--
-- Name: redemptions redemption_id; Type: DEFAULT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.redemptions ALTER COLUMN redemption_id SET DEFAULT nextval('public.redemptions_redemption_id_seq'::regclass);


--
-- Name: voucher_requests request_id; Type: DEFAULT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_requests ALTER COLUMN request_id SET DEFAULT nextval('public.voucher_requests_request_id_seq'::regclass);


--
-- Name: voucher_stores voucher_store_id; Type: DEFAULT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_stores ALTER COLUMN voucher_store_id SET DEFAULT nextval('public.voucher_stores_voucher_store_id_seq'::regclass);


--
-- Data for Name: audit_trail; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.audit_trail (audit_id, action_type, entity_type, entity_id, user_name, action_description, old_value, new_value, "timestamp", ip_address, context) FROM stdin;
1	CREATE	VOUCHER_REQUEST	VR0003	Test_Superuser	Created voucher request for Nicholas Tesla with 1 vouchers	\N	VR0003	2025-12-11 09:41:08.799	\N	Request creation
2	CREATE	VOUCHER_REQUEST	VR0004	Test_Superuser	Created voucher request for Nicholas Tesla with 1 vouchers	\N	VR0004	2025-12-11 09:50:19.477	\N	Request creation
3	CREATE	VOUCHER_REQUEST	VR0005	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0005	2025-12-11 09:51:27.572	\N	Request creation
4	UPDATE	PAYMENT	VR0005	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-11 09:52:00.569	\N	Payment update
5	APPROVE	VOUCHER_REQUEST	VR0005	Test_Superuser	Voucher request approved	pending	approved	2025-12-11 09:52:13.761	\N	Approval
6	GENERATE	VOUCHERS	VR0005	Test_Superuser	Generated 1 vouchers	\N	1	2025-12-11 09:52:52.131	\N	Voucher generation
7	SEND	VOUCHERS	VR0005	Test_Superuser	Vouchers sent to yo16.snip@gmail.com	\N	yo16.snip@gmail.com	2025-12-11 09:52:54.098	\N	Email dispatch
8	DELETE	VOUCHER_REQUEST	VR0001	Test_Superuser	Deleted voucher request	\N	\N	2025-12-11 10:07:54.46	\N	Request deletion
9	DELETE	VOUCHER_REQUEST	VR0002	Test_Superuser	Deleted voucher request	\N	\N	2025-12-11 10:08:03.648	\N	Request deletion
10	DELETE	VOUCHER_REQUEST	VR0003	Test_Superuser	Deleted voucher request	\N	\N	2025-12-11 10:08:12.051	\N	Request deletion
11	DELETE	VOUCHER_REQUEST	VR0004	Test_Superuser	Deleted voucher request	\N	\N	2025-12-11 10:08:20.971	\N	Request deletion
12	DELETE	VOUCHER_REQUEST	VR0005	Test_Superuser	Deleted voucher request	\N	\N	2025-12-11 10:08:29.68	\N	Request deletion
13	CREATE	VOUCHER_REQUEST	VCHR000001	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000001	2025-12-11 10:12:12.313	\N	Request creation
14	UPDATE	PAYMENT	VCHR000001	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-11 10:13:19.599	\N	Payment update
15	APPROVE	VOUCHER_REQUEST	VCHR000001	Test_Superuser	Voucher request approved	pending	approved	2025-12-11 10:13:38.268	\N	Approval
16	CREATE	VOUCHER_REQUEST	VCHR000002	Test_Superuser	Created voucher request for Nicholas Tesla with 2 vouchers	\N	VCHR000002	2025-12-11 10:15:38.535	\N	Request creation
17	UPDATE	PAYMENT	VCHR000002	Test_Superuser	Payment status changed from unpaid to unpaid	unpaid	unpaid	2025-12-11 10:16:02.988	\N	Payment update
18	CREATE	VOUCHER_REQUEST	VCHR000004	Test_Superuser	Created voucher request for yo with 3 vouchers	\N	VCHR000004	2025-12-11 10:27:35.397	\N	Request creation
19	UPDATE	PAYMENT	VCHR000004	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-11 10:28:06.313	\N	Payment update
20	APPROVE	VOUCHER_REQUEST	VCHR000004	Test_Superuser	Voucher request approved	pending	approved	2025-12-11 10:28:21.363	\N	Approval
41	REDEEM	VOUCHER	VCHR000004	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-11 15:01:47.349	\N	Redemption
42	REDEEM	VOUCHER	VCHR000005	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-11 15:04:50.135	\N	Redemption
43	REDEEM	VOUCHER	VCHR000006	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-11 15:05:22.162	\N	Redemption
44	REDEEM	VOUCHER	VCHR000001	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-11 15:22:21.117	\N	Redemption
45	CREATE	VOUCHER_REQUEST	VCHR000007	Test_Superuser	Created voucher request for Bruno with 2 vouchers	\N	VCHR000007	2025-12-11 16:55:34.722	\N	Request creation
46	UPDATE	PAYMENT	VCHR000007	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-11 16:56:40.491	\N	Payment update
47	APPROVE	VOUCHER_REQUEST	VCHR000007	Test_Superuser	Voucher request approved	pending	approved	2025-12-11 16:57:41.527	\N	Approval
48	REDEEM	VOUCHER	VCHR000007	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-11 17:02:56.089	\N	Redemption
49	REDEEM	VOUCHER	VCHR000008	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-11 17:04:44.952	\N	Redemption
50	APPROVE	VOUCHER_REQUEST	VCHR000002	Test_Superuser	Voucher request approved	pending	approved	2025-12-13 15:33:17.718	\N	Approval
101	UPDATE	PAYMENT	VR0001	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 14:48:20.593	\N	Payment update
51	CREATE	VOUCHER_REQUEST	VCHR000009	Test_Superuser	Created voucher request for Jimmy with 2 vouchers	\N	VCHR000009	2025-12-13 17:09:24.551	\N	Request creation
52	UPDATE	PAYMENT	VCHR000009	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-13 17:10:39.131	\N	Payment update
53	APPROVE	VOUCHER_REQUEST	VCHR000009	Test_Superuser	Voucher request approved	pending	approved	2025-12-13 17:11:04.844	\N	Approval
54	CREATE	VOUCHER_REQUEST	VCHR000011	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000011	2025-12-13 17:14:03.847	\N	Request creation
55	UPDATE	PAYMENT	VCHR000011	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-13 17:15:02.046	\N	Payment update
56	APPROVE	VOUCHER_REQUEST	VCHR000011	Test_Superuser	Voucher request approved	pending	approved	2025-12-13 17:15:25.748	\N	Approval
57	REDEEM	VOUCHER	VCHR000011	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-13 17:18:15.711	\N	Redemption
58	CREATE	VOUCHER_REQUEST	VCHR000012	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000012	2025-12-14 11:24:13.727	\N	Request creation
59	UPDATE	PAYMENT	VCHR000012	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-14 11:25:06.665	\N	Payment update
60	APPROVE	VOUCHER_REQUEST	VCHR000012	Test_Superuser	Voucher request approved	pending	approved	2025-12-14 11:25:43.218	\N	Approval
61	REDEEM	VOUCHER	VCHR000012	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-14 11:27:32.833	\N	Redemption
62	CREATE	VOUCHER_REQUEST	VCHR000013	Test_Superuser	Created voucher request for Nicholas Tesla with 1 vouchers	\N	VCHR000013	2025-12-14 14:57:13.476	\N	Request creation
63	UPDATE	PAYMENT	VCHR000013	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-14 14:58:09.521	\N	Payment update
64	APPROVE	VOUCHER_REQUEST	VCHR000013	Test_Superuser	Voucher request approved	pending	approved	2025-12-14 14:58:31.737	\N	Approval
65	REDEEM	VOUCHER	VCHR000013	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-14 15:00:35.594	\N	Redemption
66	REDEEM	VOUCHER	VCHR000002	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-19 17:20:43.956	\N	Redemption
67	REDEEM	VOUCHER	VCHR000003	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-19 17:22:30.53	\N	Redemption
68	CREATE	VOUCHER_REQUEST	VCHR000014	Test_Superuser	Created voucher request for yo with 2 vouchers	\N	VCHR000014	2025-12-19 17:33:07.88	\N	Request creation
69	UPDATE	PAYMENT	VCHR000014	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-19 17:36:07.673	\N	Payment update
70	APPROVE	VOUCHER_REQUEST	VCHR000014	Test_Superuser	Voucher request approved	pending	approved	2025-12-19 17:36:34.656	\N	Approval
71	REDEEM	VOUCHER	VCHR000014	Test_Approver	Voucher redeemed at Jean paul	active	redeemed	2025-12-19 17:39:45.904	\N	Redemption
72	REDEEM	VOUCHER	VCHR000015	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2025-12-19 17:41:29.653	\N	Redemption
73	REDEEM	VOUCHER	VCHR000009	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-19 18:30:27.013	\N	Redemption
74	REDEEM	VOUCHER	VCHR000010	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-19 18:31:31.249	\N	Redemption
75	CREATE	VOUCHER_REQUEST	VCHR000016	Test_Superuser	Created voucher request for Nicholas Tesla with 10 vouchers	\N	VCHR000016	2025-12-20 06:41:00.985	\N	Request creation
76	UPDATE	PAYMENT	VCHR000016	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2025-12-20 06:56:37.034	\N	Payment update
77	APPROVE	VOUCHER_REQUEST	VCHR000016	Test_Superuser	Voucher request approved	pending	approved	2025-12-20 06:56:51.056	\N	Approval
78	REDEEM	VOUCHER	VCHR000016	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-20 06:59:22.392	\N	Redemption
79	REDEEM	VOUCHER	VCHR000018	Test_Approver	Voucher redeemed at Topo Grill	active	redeemed	2025-12-20 07:00:20	\N	Redemption
80	REDEEM	VOUCHER	VCHR000019	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-20 07:01:03.015	\N	Redemption
81	REDEEM	VOUCHER	VCHR000021	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-20 07:02:16.621	\N	Redemption
82	REDEEM	VOUCHER	VCHR000022	Test_Approver	Voucher redeemed at Mado	active	redeemed	2025-12-20 07:03:00.272	\N	Redemption
83	REDEEM	VOUCHER	VCHR000023	Test_Approver	Voucher redeemed at Insomnia	active	redeemed	2025-12-20 07:03:47.551	\N	Redemption
84	REDEEM	VOUCHER	VCHR000025	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-20 07:04:21.935	\N	Redemption
85	REDEEM	VOUCHER	VCHR000017	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-20 07:05:13.205	\N	Redemption
86	REDEEM	VOUCHER	VCHR000020	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2025-12-20 07:05:51.118	\N	Redemption
87	REDEEM	VOUCHER	VCHR000024	Test_Approver	Voucher redeemed at Phydra	active	redeemed	2025-12-20 07:07:03.494	\N	Redemption
88	CREATE	VOUCHER_REQUEST	VCHR000026	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000026	2025-12-24 15:15:28.046	\N	Request creation
89	CREATE	VOUCHER_REQUEST	VCHR000027	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000027	2026-01-01 10:33:02.811	\N	Request creation
90	UPDATE	PAYMENT	VCHR000027	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-01 10:33:37.796	\N	Payment update
91	APPROVE	VOUCHER_REQUEST	VCHR000027	Test_Superuser	Voucher request approved	pending	approved	2026-01-01 10:34:02.162	\N	Approval
92	REDEEM	VOUCHER	VCHR000027	Test_Approver	Voucher redeemed at Mado	active	redeemed	2026-01-01 10:38:46.767	\N	Redemption
93	REJECT	VOUCHER_REQUEST	VCHR000026	Test_Superuser	Rejected voucher request	\N	\N	2026-01-04 14:22:01.571	\N	Request rejection
94	UPDATE	VOUCHER_REQUEST	VCHR000026	Test_Superuser	Updated voucher request	\N	1 vouchers @ Rs 600.0	2026-01-04 14:27:18.697	\N	Request update
95	UPDATE	PAYMENT	VCHR000026	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 14:36:17.596	\N	Payment update
96	APPROVE	VOUCHER_REQUEST	VCHR000026	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 14:36:30.488	\N	Approval
97	REDEEM	VOUCHER	VCHR000026	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2026-01-04 14:37:40.308	\N	Redemption
98	CREATE	VOUCHER_REQUEST	VCHR000028	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000028	2026-01-04 14:39:26.035	\N	Request creation
99	REJECT	VOUCHER_REQUEST	VCHR000028	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 14:40:21.515	\N	Request rejection
100	CREATE	VOUCHER_REQUEST	VR0001	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0001	2026-01-04 14:47:23.601	\N	Request creation
102	APPROVE	VOUCHER_REQUEST	VR0001	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 14:48:43.053	\N	Approval
103	REDEEM	VOUCHER	VCHR000028	Test_Approver	Voucher redeemed at Tipo Grill	active	redeemed	2026-01-04 14:49:51.138	\N	Redemption
104	CREATE	VOUCHER_REQUEST	VCHR000029	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000029	2026-01-04 14:51:45.374	\N	Request creation
105	CREATE	VOUCHER_REQUEST	VCHR000030	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000030	2026-01-04 14:52:24.472	\N	Request creation
106	REJECT	VOUCHER_REQUEST	VCHR000029	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 14:52:36.622	\N	Request rejection
107	REJECT	VOUCHER_REQUEST	VCHR000030	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 14:52:55.453	\N	Request rejection
108	CREATE	VOUCHER_REQUEST	VR0002	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0002	2026-01-04 14:53:19.12	\N	Request creation
109	CREATE	VOUCHER_REQUEST	VR0003	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0003	2026-01-04 14:53:42.772	\N	Request creation
110	CREATE	VOUCHER_REQUEST	VCHR000031	Test_Superuser	Created voucher request for Sam with 1 vouchers	\N	VCHR000031	2026-01-04 14:53:59.75	\N	Request creation
111	REJECT	VOUCHER_REQUEST	VCHR000031	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 14:54:24.887	\N	Request rejection
112	CREATE	VOUCHER_REQUEST	VR0004	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0004	2026-01-04 14:54:48.923	\N	Request creation
113	REJECT	VOUCHER_REQUEST	VR0002	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 14:56:45.277	\N	Request rejection
114	CREATE	VOUCHER_REQUEST	VR0005	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0005	2026-01-04 14:57:12.892	\N	Request creation
115	CREATE	VOUCHER_REQUEST	VCHR000032	Test_Superuser	Created voucher request for Sam with 1 vouchers	\N	VCHR000032	2026-01-04 14:58:10.123	\N	Request creation
116	UPDATE	PAYMENT	VR0004	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 15:00:36.073	\N	Payment update
117	REJECT	VOUCHER_REQUEST	VR0004	Test_Superuser	Rejected voucher request	\N	\N	2026-01-04 15:01:07.333	\N	Request rejection
118	UPDATE	PAYMENT	VR0003	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 15:04:43.171	\N	Payment update
119	APPROVE	VOUCHER_REQUEST	VR0003	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 15:05:01.315	\N	Approval
120	REDEEM	VOUCHER	VCHR000030	Test_Approver	Voucher redeemed at Zantakwanka	active	redeemed	2026-01-04 15:06:01.77	\N	Redemption
121	UPDATE	PAYMENT	VR0005	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 15:12:01.407	\N	Payment update
122	APPROVE	VOUCHER_REQUEST	VR0005	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 15:19:15.624	\N	Approval
123	REDEEM	VOUCHER	VCHR000029	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2026-01-04 15:22:44.58	\N	Redemption
124	CREATE	VOUCHER_REQUEST	VR0006	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0006	2026-01-04 15:23:59.733	\N	Request creation
125	REJECT	VOUCHER_REQUEST	VR0006	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 15:24:35.609	\N	Request rejection
126	CREATE	VOUCHER_REQUEST	VR0007	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VR0007	2026-01-04 16:01:37.377	\N	Request creation
127	UPDATE	PAYMENT	VCHR000032	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 16:09:25.497	\N	Payment update
128	APPROVE	VOUCHER_REQUEST	VCHR000032	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 16:10:07.185	\N	Approval
129	UPDATE	PAYMENT	VR0007	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 16:22:30	\N	Payment update
130	APPROVE	VOUCHER_REQUEST	VR0007	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 16:22:44.69	\N	Approval
131	REDEEM	VOUCHER	VCHR000031	Test_Approver	Voucher redeemed at Jean Paul	active	redeemed	2026-01-04 16:26:16.083	\N	Redemption
132	CREATE	VOUCHER_REQUEST	VCHR000033	Test_Superuser	Created voucher request for yo with 2 vouchers	\N	VCHR000033	2026-01-04 16:34:35.834	\N	Request creation
133	UPDATE	PAYMENT	VCHR000033	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-01-04 16:35:13.327	\N	Payment update
134	APPROVE	VOUCHER_REQUEST	VCHR000033	Test_Superuser	Voucher request approved	pending	approved	2026-01-04 16:35:42.972	\N	Approval
135	CREATE	VOUCHER_REQUEST	VCHR000035	Test_Superuser	Created voucher request for yo with 1 vouchers	\N	VCHR000035	2026-01-04 16:36:35.881	\N	Request creation
136	REJECT	VOUCHER_REQUEST	VCHR000035	Test_Superuser	Rejected voucher request (reason: payment status)	\N	\N	2026-01-04 16:36:49.401	\N	Request rejection
137	CREATE	VOUCHER_REQUEST	VR0008	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VR0008	2026-01-04 17:36:00.917	\N	Request creation
138	CREATE	VOUCHER_REQUEST	VCHR000036	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000036	2026-02-05 19:16:57.04	\N	Request creation
139	CREATE	VOUCHER_REQUEST	VCHR000037	Test_Superuser	Created voucher request for yo with 2 vouchers	\N	VCHR000037	2026-03-29 17:41:48.272	\N	Request creation
140	UPDATE	PAYMENT	VCHR000037	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-03-29 17:42:51.765	\N	Payment update
141	APPROVE	VOUCHER_REQUEST	VCHR000037	Test_Superuser	Voucher request approved	pending	approved	2026-03-29 17:43:13.446	\N	Approval
142	UPDATE	PAYMENT	VCHR000036	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-03-29 17:47:14.942	\N	Payment update
143	APPROVE	VOUCHER_REQUEST	VCHR000036	Test_Superuser	Voucher request approved	pending	approved	2026-03-29 17:48:11.956	\N	Approval
144	UPDATE	PAYMENT	VR0008	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-03-29 17:55:45.561	\N	Payment update
145	APPROVE	VOUCHER_REQUEST	VR0008	Test_Superuser	Voucher request approved	pending	approved	2026-03-29 17:56:13.294	\N	Approval
146	REDEEM	VOUCHER	VCHR000035	Bruno	Voucher redeemed at Jean Paul	active	redeemed	2026-03-29 18:14:11.454	\N	Redemption
147	CREATE	VOUCHER_REQUEST	VCHR000039	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000039	2026-03-30 11:22:05.695	\N	Request creation
148	UPDATE	PAYMENT	VCHR000039	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-03-30 11:23:13.093	\N	Payment update
149	APPROVE	VOUCHER_REQUEST	VCHR000039	Test_Superuser	Voucher request approved	pending	approved	2026-03-30 11:24:50.773	\N	Approval
150	REDEEM	VOUCHER	VCHR000039	Bruno	Voucher redeemed at Jean Paul	active	redeemed	2026-03-30 11:26:14.445	\N	Redemption
151	CREATE	VOUCHER_REQUEST	VCHR000040	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000040	2026-03-30 11:29:13.586	\N	Request creation
152	UPDATE	PAYMENT	VCHR000040	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-03-30 11:30:02.553	\N	Payment update
153	APPROVE	VOUCHER_REQUEST	VCHR000040	Test_Superuser	Voucher request approved	pending	approved	2026-03-30 11:31:12.79	\N	Approval
154	REDEEM	VOUCHER	VCHR000040	Bruno	Voucher redeemed at Jean Paul	active	redeemed	2026-03-30 12:00:25.329	\N	Redemption
155	CREATE	VOUCHER_REQUEST	VCHR000041	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000041	2026-04-09 14:09:48.127	\N	Request creation
156	CREATE	VOUCHER_REQUEST	VCHR000042	Test_Superuser	Created voucher request for Paul with 1 vouchers	\N	VCHR000042	2026-04-20 19:40:22.793	\N	Request creation
157	CREATE	VOUCHER_REQUEST	VCHR000043	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000043	2026-04-20 19:41:17.675	\N	Request creation
158	UPDATE	PAYMENT	VCHR000043	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-04-20 19:43:19.473	\N	Payment update
159	APPROVE	VOUCHER_REQUEST	VCHR000043	Test_Superuser	Voucher request approved	pending	approved	2026-04-20 19:43:47.903	\N	Approval
160	CREATE	VOUCHER_REQUEST	VCHR000044	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000044	2026-04-20 20:23:01.536	\N	Request creation
161	UPDATE	PAYMENT	VCHR000044	Test_Superuser	Payment status changed from unpaid to unpaid	unpaid	unpaid	2026-04-20 20:24:11.857	\N	Payment update
162	UPDATE	PAYMENT	VCHR000044	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-04-20 20:24:50.313	\N	Payment update
163	APPROVE	VOUCHER_REQUEST	VCHR000044	Test_Superuser	Voucher request approved	pending	approved	2026-04-20 20:25:03.7	\N	Approval
164	CREATE	VOUCHER_REQUEST	VCHR000045	Test_Superuser	Created voucher request for Bruno with 1 vouchers	\N	VCHR000045	2026-04-20 20:35:15.039	\N	Request creation
165	UPDATE	PAYMENT	VCHR000045	Test_Superuser	Payment status changed from unpaid to paid	unpaid	paid	2026-04-20 20:35:58.75	\N	Payment update
166	APPROVE	VOUCHER_REQUEST	VCHR000045	Test_Superuser	Voucher request approved	pending	approved	2026-04-20 20:36:21.194	\N	Approval
167	APPROVE	VOUCHER_REQUEST	VCHR000044	Test_Superuser	Voucher request approved	pending	approved	2026-04-20 21:03:43.469	\N	Approval
\.


--
-- Data for Name: branch; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.branch (branch_id, location, responsible_user, ref_company, address_branch, phone_branch, company, phone, industry) FROM stdin;
6	Grand Baie	yoyo	\N	\N	\N	Jean Paul	1324567	\N
10	Grand Baie	yoyo	\N	\N	\N	Tipo Grill	12367891	Food & Beverages
12	Disney Land	Test_Admin	\N	\N	\N	Twinky	0000000000	Entertainment
\.


--
-- Data for Name: clients; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.clients (ref_client, nom_client, email_client, address_client, phone_client) FROM stdin;
5	Sam	sam@mail.com	DC	97954652100
3	Jimmy	jim@mail.com	Vacoas	1223344
2	Nicholas Tesla	mmm@vms.com	Goodlands	59090909
7	Paul	paul@vms.com	Plaisance	56790900
1	yo	test4564747@gmail.com	Did	12232454
6	Jean	Jean@vms.com	Grand Baie	14236786
4	Bruno	julien.projts@gmail.com	kinder bruno	55550000
\.


--
-- Data for Name: company; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.company (company_id, name_company, email_company, industry_type) FROM stdin;
1	Artisans & Co	artisan@mail.com	Foods & Drinks
2	Apparels Corp	apparel@mail.com	Clothing
4	Technopholia	techpholia@mail.com	Technology
\.


--
-- Data for Name: invoices; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.invoices (invoice_id, invoice_number, request_id, request_reference, ref_client, client_name, total_amount, status, created_at) FROM stdin;
10	INV-VCHR000001	17	VCHR000001	1	yo	1000.00	paid	2025-12-11 10:12:11.693328
12	INV-VCHR000004	19	VCHR000004	1	yo	450.00	paid	2025-12-11 10:27:34.283098
13	INV-VCHR000007	20	VCHR000007	4	Bruno	1000.00	paid	2025-12-11 16:55:28.931721
11	INV-VCHR000002	18	VCHR000002	2	Nicholas Tesla	200.00	paid	2025-12-11 10:15:37.688711
14	INV-VCHR000009	21	VCHR000009	3	Jimmy	200.00	paid	2025-12-13 17:09:23.791023
15	INV-VCHR000011	22	VCHR000011	1	yo	100.00	paid	2025-12-13 17:14:03.558119
16	INV-VCHR000012	23	VCHR000012	1	yo	900.00	paid	2025-12-14 11:24:07.575057
17	INV-VCHR000013	24	VCHR000013	2	Nicholas Tesla	500.00	paid	2025-12-14 14:57:09.986343
18	INV-VCHR000014	25	VCHR000014	1	yo	2000.00	paid	2025-12-19 17:33:04.875623
19	INV-VCHR000016	26	VCHR000016	2	Nicholas Tesla	10000.00	paid	2025-12-20 06:40:56.145767
21	INV-VCHR000027	28	VCHR000027	1	yo	1000.00	paid	2026-01-01 10:32:58.401096
20	INV-VCHR000026	27	VCHR000026	4	Bruno	600.00	paid	2025-12-24 15:15:25.710285
22	INV-VCHR000028	29	VCHR000028	1	yo	1000.00	pending	2026-01-04 14:39:21.807185
23	INV-VR0001	31	VR0001	1	yo	1000.00	paid	2026-01-04 14:47:19.342703
24	INV-VCHR000029	32	VCHR000029	1	yo	1000.00	pending	2026-01-04 14:51:41.134107
25	INV-VCHR000030	33	VCHR000030	1	yo	1100.00	pending	2026-01-04 14:52:20.190838
26	INV-VR0002	34	VR0002	1	yo	1111.00	pending	2026-01-04 14:53:14.840604
28	INV-VCHR000031	36	VCHR000031	5	Sam	1000.00	pending	2026-01-04 14:53:55.47055
29	INV-VR0004	37	VR0004	1	yo	1111.00	paid	2026-01-04 14:54:44.639808
27	INV-VR0003	35	VR0003	1	yo	1000.00	paid	2026-01-04 14:53:38.467392
30	INV-VR0005	38	VR0005	1	yo	1000.00	paid	2026-01-04 14:57:08.575345
32	INV-VR0006	40	VR0006	1	yo	30000.00	pending	2026-01-04 15:23:55.312456
31	INV-VCHR000032	39	VCHR000032	5	Sam	1000.00	paid	2026-01-04 14:58:05.818365
33	INV-VR0007	41	VR0007	1	yo	1000.00	paid	2026-01-04 16:01:32.775072
34	INV-VCHR000033	42	VCHR000033	1	yo	2000.00	paid	2026-01-04 16:34:30.884273
35	INV-VCHR000035	43	VCHR000035	1	yo	1000.00	pending	2026-01-04 16:36:31.109396
38	INV-VCHR000037	46	VCHR000037	1	yo	1000.00	paid	2026-03-29 17:41:34.151525
37	INV-VCHR000036	45	VCHR000036	4	Bruno	1000.00	paid	2026-02-05 19:16:53.963144
36	INV-VR0008	44	VR0008	4	Bruno	500.00	paid	2026-01-04 17:35:54.937636
39	INV-VCHR000039	47	VCHR000039	4	Bruno	2500.00	paid	2026-03-30 11:21:49.710782
40	INV-VCHR000040	48	VCHR000040	4	Bruno	100.00	paid	2026-03-30 11:28:57.667062
41	INV-VCHR000041	49	VCHR000041	4	Bruno	2000.00	pending	2026-04-09 14:09:45.593952
42	INV-VCHR000042	50	VCHR000042	7	Paul	50.00	pending	2026-04-20 19:40:20.596026
43	INV-VCHR000043	51	VCHR000043	4	Bruno	50.00	paid	2026-04-20 19:41:15.464357
44	INV-VCHR000044	52	VCHR000044	4	Bruno	1200.00	paid	2026-04-20 20:22:58.00341
45	INV-VCHR000045	53	VCHR000045	4	Bruno	1200.00	paid	2026-04-20 20:35:11.459078
\.


--
-- Data for Name: redemption_audit; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.redemption_audit (redemption_id, redemption_time, status, branch, voucher_code, redeemed_by, message) FROM stdin;
1	2026-03-30 11:26:02.653082	SUCCESS	Jean Paul	VCHR000039	Bruno	Voucher redeemed successfully!
2	2026-03-30 11:26:49.150657	FAILED	Jean Paul	VCHR000114	Bruno	Voucher has expired.
3	2026-03-30 12:00:13.538749	SUCCESS	Jean Paul	VCHR000040	Bruno	Voucher redeemed successfully!
4	2026-03-30 12:02:29.237117	FAILED	Jean Paul	VCHR000117	Bruno	Voucher has expired.
5	2026-04-20 15:46:45.470889	SUCCESS	Tipo Grill	VCHR000043	Bruno	Voucher redeemed successfully!
6	2026-04-20 16:40:10.125806	SUCCESS	Tipo Grill	VCHR000045	Bruno	Voucher redeemed successfully!
7	2026-04-20 17:06:59.858585	SUCCESS	Tipo Grill	VCHR000044	Bruno	Voucher redeemed successfully!
\.


--
-- Data for Name: redemptions; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.redemptions (redemption_id, voucher_code, branch_id, branch_location, redeemed_by, redemption_date, redemption_time, status, notes) FROM stdin;
\.


--
-- Data for Name: requests; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.requests (ref_request, creation_date, num_voucher, status, payment, date_payment, ref_payment, date_approval, duration_voucher, ref_client, processed_by, approved_by, validated_by, request_reference, client_name, unit_value, total_value, payment_status, approver_email, invoice_id, vouchers_generated, vouchers_sent) FROM stdin;
1	2025-12-07	5	CREATED	\N	\N	\N	\N	0	3	\N	\N	\N	\N	\N	\N	\N	unpaid	\N	\N	f	f
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.users (username, first_name_user, last_name_user, email_user, role, password, ddl, titre, status, company) FROM stdin;
Matt.O	Mathieu	Opman	matt@mail.com	Supervisor	12345	\N	Supervisor	Active	\N
Edith.D	Edith	Dave	edith@mail.com	Supervisor	12345	\N	Supervisor_Quicksilver	Active	\N
Test_Superuser	Supergo	Test_Surname	superuser@vms.com	Superuser	123789	\N	Superuser	Active	\N
Test_Approver	Apparadu	Approver_Surname	approver@vms.com	Approver	14235	\N	Approver	Active	\N
Gigs.B	Gigson	Butcher	gigs@mail.com	Supervisor	12345		Supervisor_Techworld	Active	
Test_Admin	Admin	Admin_Surname	admin@vms.com	Admin	12345		Admin	Active	
Test_Accountant	Bruno	Accountant_Surname	accountant@vms.com	Accountant	12345		Accountant	Active	
yoyo	yoyoyo	yoyyooyy	yo@gmail.com	Approver	111111		The Goat	Inactive	Twinky
\.


--
-- Data for Name: voucher_requests; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.voucher_requests (request_id, request_reference, ref_client, client_name, num_vouchers, unit_value, total_value, status, payment_status, payment_date, approved_by, approval_date, processed_by, created_at, updated_at, client_email, vouchers_generated, vouchers_sent, expiration_date) FROM stdin;
29	VCHR000028	1	yo	1	1000.00	1000.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 14:40:17.682602	Test_Superuser	2026-01-04 14:39:21.377783	2026-01-04 14:40:17.682602	\N	f	f	\N
22	VCHR000011	1	yo	1	100.00	100.00	Redeemed	paid	2025-12-13 17:14:42.442	Test_Superuser	2025-12-13 17:15:25.925393	Test_Superuser	2025-12-13 17:14:03.125304	2025-12-13 17:16:34.424974	\N	t	t	\N
51	VCHR000043	4	Bruno	1	50.00	50.00	Redeemed	paid	2026-04-20 19:42:59.503	Test_Superuser	2026-04-20 19:43:46.065823	Test_Superuser	2026-04-20 19:41:14.983963	2026-04-20 19:44:28.762875	\N	t	t	\N
31	VR0001	1	yo	1	1000.00	1000.00	Redeemed	paid	2026-01-04 14:48:09.572	Test_Superuser	2026-01-04 14:48:39.176276	Test_Superuser	2026-01-04 14:47:18.921405	2026-01-04 14:49:03.396711	\N	t	t	\N
23	VCHR000012	1	yo	1	900.00	900.00	Redeemed	paid	2025-12-14 11:24:55.897	Test_Superuser	2025-12-14 11:25:37.49507	Test_Superuser	2025-12-14 11:24:07.1471	2025-12-14 11:26:03.119565	\N	t	t	\N
32	VCHR000029	1	yo	1	1000.00	1000.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 14:52:32.733849	Test_Superuser	2026-01-04 14:51:40.713415	2026-01-04 14:52:32.733849	\N	f	f	\N
24	VCHR000013	2	Nicholas Tesla	1	500.00	500.00	Redeemed	paid	2025-12-14 14:57:48.222	Test_Superuser	2025-12-14 14:58:28.699582	Test_Superuser	2025-12-14 14:57:09.566746	2025-12-14 14:59:34.758677	\N	t	t	\N
18	VCHR000002	2	Nicholas Tesla	2	100.00	200.00	Redeemed	paid	2025-12-13 15:09:05.877	Test_Superuser	2025-12-13 15:33:09.288807	Test_Superuser	2025-12-11 10:15:37.263457	2025-12-13 17:04:48.586747	\N	t	t	\N
33	VCHR000030	1	yo	1	1100.00	1100.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 14:52:51.645324	Test_Superuser	2026-01-04 14:52:19.770879	2026-01-04 14:52:51.645324	\N	f	f	\N
19	VCHR000004	1	yo	3	150.00	450.00	Redeemed	paid	2025-12-11 10:27:57.775	Test_Superuser	2025-12-11 10:28:21.098931	Test_Superuser	2025-12-11 10:27:33.856011	2025-12-11 10:28:53.979072	\N	t	t	\N
17	VCHR000001	1	yo	1	1000.00	1000.00	Redeemed	paid	2025-12-11 10:13:09.852	Test_Superuser	2025-12-11 10:13:38.070088	Test_Superuser	2025-12-11 10:12:11.265994	2025-12-11 10:14:15.283509	\N	t	t	\N
25	VCHR000014	1	yo	2	1000.00	2000.00	Redeemed	paid	2025-12-19 17:35:56.472	Test_Superuser	2025-12-19 17:36:32.273873	Test_Superuser	2025-12-19 17:33:04.451888	2025-12-19 17:37:05.206447	\N	t	t	\N
21	VCHR000009	3	Jimmy	2	100.00	200.00	Redeemed	paid	2025-12-13 17:10:12.703	Test_Superuser	2025-12-13 17:11:04.856675	Test_Superuser	2025-12-13 17:09:23.354684	2025-12-13 17:12:46.172211	\N	t	t	\N
20	VCHR000007	4	Bruno	2	500.00	1000.00	Redeemed	paid	2025-12-11 16:56:20.514	Test_Superuser	2025-12-11 16:57:36.43995	Test_Superuser	2025-12-11 16:55:28.501937	2025-12-11 16:59:11.639578	\N	t	t	\N
36	VCHR000031	5	Sam	1	1000.00	1000.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 14:54:21.024921	Test_Superuser	2026-01-04 14:53:55.050543	2026-01-04 14:54:21.024921	\N	f	f	\N
34	VR0002	1	yo	1	1111.00	1111.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 14:56:41.405851	Test_Superuser	2026-01-04 14:53:14.412897	2026-01-04 14:56:41.405851	\N	f	f	\N
26	VCHR000016	2	Nicholas Tesla	10	1000.00	10000.00	Redeemed	paid	2025-12-20 06:56:26.44	Test_Superuser	2025-12-20 06:56:48.541648	Test_Superuser	2025-12-20 06:40:55.718196	2025-12-20 06:57:16.529373	\N	t	t	\N
41	VR0007	1	yo	1	1000.00	1000.00	Redeemed	paid	2026-01-04 16:22:19.007	Test_Superuser	2026-01-04 16:22:40.399767	Test_Superuser	2026-01-04 16:01:32.353193	2026-01-04 16:23:19.211039	\N	t	t	\N
37	VR0004	1	yo	1	1111.00	1111.00	rejected	paid	2026-01-04 15:00:24.862	Test_Superuser	2026-01-04 15:01:03.468245	Test_Superuser	2026-01-04 14:54:44.219141	2026-01-04 15:01:03.468245	\N	f	f	\N
28	VCHR000027	1	yo	1	1000.00	1000.00	Redeemed	paid	2026-01-01 10:33:25.955	Test_Superuser	2026-01-01 10:33:58.208656	Test_Superuser	2026-01-01 10:32:57.978957	2026-01-01 10:34:25.737596	\N	t	t	\N
47	VCHR000039	4	Bruno	1	2500.00	2500.00	Redeemed	paid	2026-03-30 11:22:51.972	Test_Superuser	2026-03-30 11:24:35.228114	Test_Superuser	2026-03-30 11:21:49.310415	2026-03-30 11:25:18.37297	\N	t	t	\N
35	VR0003	1	yo	1	1000.00	1000.00	Redeemed	paid	2026-01-04 15:04:33.176	Test_Superuser	2026-01-04 15:04:57.357251	Test_Superuser	2026-01-04 14:53:38.055096	2026-01-04 15:05:21.696029	\N	t	t	\N
42	VCHR000033	1	yo	2	1000.00	2000.00	completed	paid	2026-01-04 16:35:01.618	Test_Superuser	2026-01-04 16:35:38.640059	Test_Superuser	2026-01-04 16:34:30.469546	2026-01-04 16:36:15.729925	\N	t	t	\N
27	VCHR000026	4	Bruno	1	600.00	600.00	Redeemed	paid	2026-01-04 14:36:06.151	Test_Superuser	2026-01-04 14:36:26.732602	Test_Superuser	2025-12-24 15:15:25.25479	2026-01-04 14:36:56.815141	\N	t	t	\N
43	VCHR000035	1	yo	1	1000.00	1000.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 16:36:45.059035	Test_Superuser	2026-01-04 16:36:30.680624	2026-01-04 16:36:45.059035	\N	f	f	\N
38	VR0005	1	yo	1	1000.00	1000.00	Redeemed	paid	2026-01-04 15:11:50.651	Test_Superuser	2026-01-04 15:19:11.638928	Test_Superuser	2026-01-04 14:57:08.146356	2026-01-04 15:21:37.912962	\N	t	t	\N
40	VR0006	1	yo	1	30000.00	30000.00	rejected	unpaid	\N	Test_Superuser	2026-01-04 15:24:31.752966	Test_Superuser	2026-01-04 15:23:54.892175	2026-01-04 15:24:31.752966	\N	f	f	\N
39	VCHR000032	5	Sam	1	1000.00	1000.00	completed	paid	2026-01-04 16:09:15.205	Test_Superuser	2026-01-04 16:10:02.974643	Test_Superuser	2026-01-04 14:58:05.398517	2026-01-04 16:21:14.67891	\N	t	t	\N
46	VCHR000037	1	yo	2	500.00	1000.00	completed	paid	2026-03-29 17:42:29.883	Test_Superuser	2026-03-29 17:43:00.072395	Test_Superuser	2026-03-29 17:41:33.699513	2026-03-29 17:46:07.832826	\N	t	t	\N
45	VCHR000036	4	Bruno	1	1000.00	1000.00	completed	paid	2026-03-29 17:46:53.566	Test_Superuser	2026-03-29 17:47:58.427878	Test_Superuser	2026-02-05 19:16:53.546828	2026-03-29 17:52:46.145741	\N	t	t	\N
48	VCHR000040	4	Bruno	1	100.00	100.00	Redeemed	paid	2026-03-30 11:29:42.76	Test_Superuser	2026-03-30 11:30:57.231441	Test_Superuser	2026-03-30 11:28:57.276147	2026-03-30 11:31:43.599141	\N	t	t	\N
49	VCHR000041	4	Bruno	1	2000.00	2000.00	initiated	unpaid	\N	\N	\N	Test_Superuser	2026-04-09 14:09:45.173906	2026-04-09 14:09:45.173906	\N	f	f	\N
50	VCHR000042	7	Paul	1	50.00	50.00	initiated	unpaid	\N	\N	\N	Test_Superuser	2026-04-20 19:40:20.197175	2026-04-20 19:40:20.197175	\N	f	f	\N
44	VR0008	4	Bruno	1	500.00	500.00	Redeemed	paid	2026-03-29 17:55:25.337	Test_Superuser	2026-03-29 17:55:59.968969	Test_Superuser	2026-01-04 17:35:54.475188	2026-03-29 17:56:52.797485	\N	t	t	\N
52	VCHR000044	4	Bruno	1	1200.00	1200.00	Redeemed	paid	2026-04-20 20:24:46.55	Test_Superuser	2026-04-20 21:03:40.11713	Test_Superuser	2026-04-20 20:22:57.62421	2026-04-20 21:04:08.130336	\N	t	t	\N
53	VCHR000045	4	Bruno	1	1200.00	1200.00	Redeemed	paid	2026-04-20 20:35:48.292	Test_Superuser	2026-04-20 20:36:17.981102	Test_Superuser	2026-04-20 20:35:11.064287	2026-04-20 20:38:28.353924	\N	t	t	\N
\.


--
-- Data for Name: voucher_stores; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.voucher_stores (voucher_store_id, voucher_code, branch_id, company, created_at) FROM stdin;
\.


--
-- Data for Name: vouchers; Type: TABLE DATA; Schema: public; Owner: julienjeanpierre
--

COPY public.vouchers (ref_voucher, val_voucher, init_date, expiry_date, status_voucher, date_redeemed, bearer, ref_request, redeemed_by, redeemed_branch, ref_client, redeemed, code_voucher, price, request_id, request_reference, assigned_to_request, qr_code_data, pdf_path, email_sent, email_sent_date) FROM stdin;
50	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000046	\N	\N	\N	f	\N	\N	f	\N
51	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000047	\N	\N	\N	f	\N	\N	f	\N
52	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000048	\N	\N	\N	f	\N	\N	f	\N
53	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000049	\N	\N	\N	f	\N	\N	f	\N
54	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000050	\N	\N	\N	f	\N	\N	f	\N
55	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000051	\N	\N	\N	f	\N	\N	f	\N
56	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000052	\N	\N	\N	f	\N	\N	f	\N
57	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000053	\N	\N	\N	f	\N	\N	f	\N
58	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000054	\N	\N	\N	f	\N	\N	f	\N
59	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000055	\N	\N	\N	f	\N	\N	f	\N
60	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000056	\N	\N	\N	f	\N	\N	f	\N
61	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000057	\N	\N	\N	f	\N	\N	f	\N
62	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000058	\N	\N	\N	f	\N	\N	f	\N
63	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000059	\N	\N	\N	f	\N	\N	f	\N
64	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000060	\N	\N	\N	f	\N	\N	f	\N
65	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000061	\N	\N	\N	f	\N	\N	f	\N
66	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000062	\N	\N	\N	f	\N	\N	f	\N
67	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000063	\N	\N	\N	f	\N	\N	f	\N
68	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000064	\N	\N	\N	f	\N	\N	f	\N
69	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000065	\N	\N	\N	f	\N	\N	f	\N
70	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000066	\N	\N	\N	f	\N	\N	f	\N
71	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000067	\N	\N	\N	f	\N	\N	f	\N
72	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000068	\N	\N	\N	f	\N	\N	f	\N
73	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000069	\N	\N	\N	f	\N	\N	f	\N
74	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000070	\N	\N	\N	f	\N	\N	f	\N
VR0005-1	5000	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	1	f	\N	\N	\N	VR0005	f	\N	E:\\IntelliJ IDEA 2025.2.5\\VMS - Vamos\\Test\\vouchers\\VR0005\\Voucher_VR0005-1.pdf	f	\N
75	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000071	\N	\N	\N	f	\N	\N	f	\N
76	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000072	\N	\N	\N	f	\N	\N	f	\N
77	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000073	\N	\N	\N	f	\N	\N	f	\N
78	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000074	\N	\N	\N	f	\N	\N	f	\N
79	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000075	\N	\N	\N	f	\N	\N	f	\N
80	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000076	\N	\N	\N	f	\N	\N	f	\N
81	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000077	\N	\N	\N	f	\N	\N	f	\N
82	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000078	\N	\N	\N	f	\N	\N	f	\N
83	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000079	\N	\N	\N	f	\N	\N	f	\N
84	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000080	\N	\N	\N	f	\N	\N	f	\N
85	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000081	\N	\N	\N	f	\N	\N	f	\N
86	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000082	\N	\N	\N	f	\N	\N	f	\N
87	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000083	\N	\N	\N	f	\N	\N	f	\N
88	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000084	\N	\N	\N	f	\N	\N	f	\N
89	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000085	\N	\N	\N	f	\N	\N	f	\N
90	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000086	\N	\N	\N	f	\N	\N	f	\N
91	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000087	\N	\N	\N	f	\N	\N	f	\N
92	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000088	\N	\N	\N	f	\N	\N	f	\N
93	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000089	\N	\N	\N	f	\N	\N	f	\N
94	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000090	\N	\N	\N	f	\N	\N	f	\N
9	150	2025-12-11	2026-12-11	Redeemed	2025-12-11	\N	\N	Test_Approver	Tipo Grill	1	t	VCHR000005	\N	19	VCHR000004	t	\N	\N	f	\N
8	150	2025-12-11	2026-12-11	Redeemed	2025-12-11	\N	\N	Test_Approver	Tipo Grill	1	t	VCHR000004	\N	19	VCHR000004	t	\N	\N	f	\N
10	150	2025-12-11	2026-12-11	Redeemed	2025-12-11	\N	\N	Test_Approver	Tipo Grill	1	t	VCHR000006	\N	19	VCHR000004	t	\N	\N	f	\N
5	1000	2025-12-11	2026-12-11	Redeemed	2025-12-11	\N	\N	Test_Approver	Jean Paul	1	t	VCHR000001	\N	17	VCHR000001	t	\N	\N	f	\N
12	500	2025-12-11	2026-12-11	Redeemed	2025-12-11	\N	\N	Test_Approver	Jean Paul	4	t	VCHR000008	\N	20	VCHR000007	t	\N	\N	f	\N
95	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000091	\N	\N	\N	f	\N	\N	f	\N
11	500	2025-12-11	2026-12-11	Redeemed	2025-12-11	\N	\N	Test_Approver	Jean Paul	4	t	VCHR000007	\N	20	VCHR000007	t	\N	\N	f	\N
96	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000092	\N	\N	\N	f	\N	\N	f	\N
97	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000093	\N	\N	\N	f	\N	\N	f	\N
98	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000094	\N	\N	\N	f	\N	\N	f	\N
99	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000095	\N	\N	\N	f	\N	\N	f	\N
100	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000096	\N	\N	\N	f	\N	\N	f	\N
101	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000097	\N	\N	\N	f	\N	\N	f	\N
102	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000098	\N	\N	\N	f	\N	\N	f	\N
103	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000099	\N	\N	\N	f	\N	\N	f	\N
14	100	2025-12-11	2026-12-11	Redeemed	2025-12-19	\N	\N	Test_Approver	Tipo Grill	3	t	VCHR000010	\N	21	VCHR000009	t	\N	\N	f	\N
30	600	2025-12-11	2026-12-11	Redeemed	2026-01-04	\N	\N	Test_Approver	Tipo Grill	4	t	VCHR000026	\N	27	VCHR000026	t	\N	\N	f	\N
13	100	2025-12-11	2026-12-11	Redeemed	2025-12-19	\N	\N	Test_Approver	Tipo Grill	3	t	VCHR000009	\N	21	VCHR000009	t	\N	\N	f	\N
15	100	2025-12-11	2026-12-11	Redeemed	2025-12-13	\N	\N	Test_Approver	Jean Paul	1	t	VCHR000011	\N	22	VCHR000011	t	\N	\N	f	\N
17	500	2025-12-11	2026-12-11	Redeemed	2025-12-14	\N	\N	Test_Approver	Jean Paul	2	t	VCHR000013	\N	24	VCHR000013	t	\N	\N	f	\N
18	1000	2025-12-11	2026-12-11	Redeemed	2025-12-19	\N	\N	Test_Approver	Jean paul	1	t	VCHR000014	\N	25	VCHR000014	t	\N	\N	f	\N
20	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Tipo Grill	2	t	VCHR000016	\N	26	VCHR000016	t	\N	\N	f	\N
34	1000	2025-12-11	2026-12-11	Redeemed	2026-01-04	\N	\N	Test_Approver	Zantakwanka	1	t	VCHR000030	\N	35	VR0003	t	\N	\N	f	\N
31	1000	2025-12-11	2026-12-11	Redeemed	2026-01-01	\N	\N	Test_Approver	Mado	1	t	VCHR000027	\N	28	VCHR000027	t	\N	\N	f	\N
32	1000	2025-12-11	2026-12-11	Redeemed	2026-01-04	\N	\N	Test_Approver	Tipo Grill	1	t	VCHR000028	\N	31	VR0001	t	\N	\N	f	\N
37	1000	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	1	f	VCHR000033	\N	42	VCHR000033	t	\N	\N	f	\N
33	1000	2025-12-11	2026-12-11	Redeemed	2026-01-04	\N	\N	Test_Approver	Jean Paul	1	t	VCHR000029	\N	38	VR0005	t	\N	\N	f	\N
36	1000	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	5	f	VCHR000032	\N	39	VCHR000032	t	\N	\N	f	\N
35	1000	2025-12-11	2026-12-11	Redeemed	2026-01-04	\N	\N	Test_Approver	Jean Paul	1	t	VCHR000031	\N	41	VR0007	t	\N	\N	f	\N
38	1000	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	1	f	VCHR000034	\N	42	VCHR000033	t	\N	\N	f	\N
45	2000	2025-12-11	2026-12-11	Reserved	\N	\N	\N	\N	\N	4	f	VCHR000041	\N	49	VCHR000041	t	\N	\N	f	\N
40	1000	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	4	f	VCHR000036	\N	45	VCHR000036	t	\N	\N	f	\N
41	500	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	1	f	VCHR000037	\N	46	VCHR000037	t	\N	\N	f	\N
39	500	2025-12-11	2026-12-11	Redeemed	2026-03-29	\N	\N	Bruno	Jean Paul	4	t	VCHR000035	\N	44	VR0008	t	\N	\N	f	\N
43	2500	2025-12-11	2026-12-11	Redeemed	2026-03-30	\N	\N	Bruno	Jean Paul	4	t	VCHR000039	\N	47	VCHR000039	t	\N	\N	f	\N
46	50	2025-12-11	2026-12-11	Reserved	\N	\N	\N	\N	\N	7	f	VCHR000042	\N	50	VCHR000042	t	\N	\N	f	\N
47	50	2025-12-11	2026-12-11	Redeemed	2026-04-20	\N	\N	Bruno	Tipo Grill	4	t	VCHR000043	\N	51	VCHR000043	t	\N	\N	f	\N
48	1200	2025-12-11	2026-12-11	Redeemed	2026-04-20	\N	\N	Bruno	Tipo Grill	4	t	VCHR000044	\N	52	VCHR000044	t	\N	\N	f	\N
104	0.0	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	\N	f	VCHR000100	\N	\N	\N	f	\N	\N	f	\N
105	0.0	2025-12-13	2026-12-13	Active	\N	\N	\N	\N	\N	\N	f	VCHR000101	\N	\N	\N	f	\N	\N	f	\N
186	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000182	\N	\N	\N	f	\N	\N	f	\N
187	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000183	\N	\N	\N	f	\N	\N	f	\N
16	900	2025-12-11	2026-12-11	Redeemed	2025-12-14	\N	\N	Test_Approver	Jean Paul	1	t	VCHR000012	\N	23	VCHR000012	t	\N	\N	f	\N
6	100	2025-12-11	2026-12-11	Redeemed	2025-12-19	\N	\N	Test_Approver	Jean Paul	2	t	VCHR000002	\N	18	VCHR000002	t	\N	\N	f	\N
7	100	2025-12-11	2026-12-11	Redeemed	2025-12-19	\N	\N	Test_Approver	Jean Paul	2	t	VCHR000003	\N	18	VCHR000002	t	\N	\N	f	\N
188	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000184	\N	\N	\N	f	\N	\N	f	\N
19	1000	2025-12-11	2026-12-11	Redeemed	2025-12-19	\N	\N	Test_Approver	Jean Paul	1	t	VCHR000015	\N	25	VCHR000014	t	\N	\N	f	\N
106	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000102	\N	\N	\N	f	\N	\N	f	\N
107	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000103	\N	\N	\N	f	\N	\N	f	\N
108	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000104	\N	\N	\N	f	\N	\N	f	\N
109	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000105	\N	\N	\N	f	\N	\N	f	\N
110	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000106	\N	\N	\N	f	\N	\N	f	\N
111	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000107	\N	\N	\N	f	\N	\N	f	\N
112	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000108	\N	\N	\N	f	\N	\N	f	\N
113	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000109	\N	\N	\N	f	\N	\N	f	\N
114	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000110	\N	\N	\N	f	\N	\N	f	\N
115	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000111	\N	\N	\N	f	\N	\N	f	\N
116	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000112	\N	\N	\N	f	\N	\N	f	\N
117	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000113	\N	\N	\N	f	\N	\N	f	\N
118	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000114	\N	\N	\N	f	\N	\N	f	\N
119	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000115	\N	\N	\N	f	\N	\N	f	\N
120	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000116	\N	\N	\N	f	\N	\N	f	\N
121	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000117	\N	\N	\N	f	\N	\N	f	\N
122	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000118	\N	\N	\N	f	\N	\N	f	\N
123	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000119	\N	\N	\N	f	\N	\N	f	\N
124	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000120	\N	\N	\N	f	\N	\N	f	\N
125	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000121	\N	\N	\N	f	\N	\N	f	\N
126	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000122	\N	\N	\N	f	\N	\N	f	\N
127	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000123	\N	\N	\N	f	\N	\N	f	\N
128	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000124	\N	\N	\N	f	\N	\N	f	\N
129	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000125	\N	\N	\N	f	\N	\N	f	\N
130	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000126	\N	\N	\N	f	\N	\N	f	\N
131	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000127	\N	\N	\N	f	\N	\N	f	\N
132	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000128	\N	\N	\N	f	\N	\N	f	\N
133	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000129	\N	\N	\N	f	\N	\N	f	\N
134	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000130	\N	\N	\N	f	\N	\N	f	\N
135	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000131	\N	\N	\N	f	\N	\N	f	\N
136	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000132	\N	\N	\N	f	\N	\N	f	\N
137	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000133	\N	\N	\N	f	\N	\N	f	\N
138	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000134	\N	\N	\N	f	\N	\N	f	\N
139	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000135	\N	\N	\N	f	\N	\N	f	\N
140	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000136	\N	\N	\N	f	\N	\N	f	\N
141	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000137	\N	\N	\N	f	\N	\N	f	\N
142	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000138	\N	\N	\N	f	\N	\N	f	\N
143	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000139	\N	\N	\N	f	\N	\N	f	\N
144	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000140	\N	\N	\N	f	\N	\N	f	\N
145	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000141	\N	\N	\N	f	\N	\N	f	\N
146	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000142	\N	\N	\N	f	\N	\N	f	\N
147	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000143	\N	\N	\N	f	\N	\N	f	\N
148	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000144	\N	\N	\N	f	\N	\N	f	\N
149	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000145	\N	\N	\N	f	\N	\N	f	\N
150	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000146	\N	\N	\N	f	\N	\N	f	\N
151	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000147	\N	\N	\N	f	\N	\N	f	\N
152	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000148	\N	\N	\N	f	\N	\N	f	\N
153	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000149	\N	\N	\N	f	\N	\N	f	\N
154	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000150	\N	\N	\N	f	\N	\N	f	\N
155	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000151	\N	\N	\N	f	\N	\N	f	\N
156	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000152	\N	\N	\N	f	\N	\N	f	\N
157	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000153	\N	\N	\N	f	\N	\N	f	\N
158	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000154	\N	\N	\N	f	\N	\N	f	\N
159	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000155	\N	\N	\N	f	\N	\N	f	\N
160	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000156	\N	\N	\N	f	\N	\N	f	\N
161	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000157	\N	\N	\N	f	\N	\N	f	\N
162	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000158	\N	\N	\N	f	\N	\N	f	\N
163	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000159	\N	\N	\N	f	\N	\N	f	\N
164	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000160	\N	\N	\N	f	\N	\N	f	\N
165	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000161	\N	\N	\N	f	\N	\N	f	\N
166	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000162	\N	\N	\N	f	\N	\N	f	\N
167	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000163	\N	\N	\N	f	\N	\N	f	\N
168	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000164	\N	\N	\N	f	\N	\N	f	\N
169	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000165	\N	\N	\N	f	\N	\N	f	\N
170	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000166	\N	\N	\N	f	\N	\N	f	\N
171	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000167	\N	\N	\N	f	\N	\N	f	\N
172	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000168	\N	\N	\N	f	\N	\N	f	\N
173	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000169	\N	\N	\N	f	\N	\N	f	\N
174	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000170	\N	\N	\N	f	\N	\N	f	\N
175	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000171	\N	\N	\N	f	\N	\N	f	\N
176	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000172	\N	\N	\N	f	\N	\N	f	\N
177	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000173	\N	\N	\N	f	\N	\N	f	\N
178	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000174	\N	\N	\N	f	\N	\N	f	\N
179	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000175	\N	\N	\N	f	\N	\N	f	\N
180	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000176	\N	\N	\N	f	\N	\N	f	\N
181	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000177	\N	\N	\N	f	\N	\N	f	\N
182	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000178	\N	\N	\N	f	\N	\N	f	\N
183	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000179	\N	\N	\N	f	\N	\N	f	\N
184	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000180	\N	\N	\N	f	\N	\N	f	\N
185	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000181	\N	\N	\N	f	\N	\N	f	\N
189	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000185	\N	\N	\N	f	\N	\N	f	\N
190	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000186	\N	\N	\N	f	\N	\N	f	\N
191	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000187	\N	\N	\N	f	\N	\N	f	\N
192	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000188	\N	\N	\N	f	\N	\N	f	\N
193	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000189	\N	\N	\N	f	\N	\N	f	\N
194	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000190	\N	\N	\N	f	\N	\N	f	\N
195	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000191	\N	\N	\N	f	\N	\N	f	\N
196	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000192	\N	\N	\N	f	\N	\N	f	\N
197	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000193	\N	\N	\N	f	\N	\N	f	\N
198	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000194	\N	\N	\N	f	\N	\N	f	\N
199	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000195	\N	\N	\N	f	\N	\N	f	\N
200	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000196	\N	\N	\N	f	\N	\N	f	\N
201	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000197	\N	\N	\N	f	\N	\N	f	\N
202	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000198	\N	\N	\N	f	\N	\N	f	\N
203	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000199	\N	\N	\N	f	\N	\N	f	\N
204	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000200	\N	\N	\N	f	\N	\N	f	\N
205	0.0	2025-12-25	2026-01-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000201	\N	\N	\N	f	\N	\N	f	\N
22	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Topo Grill	2	t	VCHR000018	\N	26	VCHR000016	t	\N	\N	f	\N
23	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Tipo Grill	2	t	VCHR000019	\N	26	VCHR000016	t	\N	\N	f	\N
25	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Tipo Grill	2	t	VCHR000021	\N	26	VCHR000016	t	\N	\N	f	\N
26	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Mado	2	t	VCHR000022	\N	26	VCHR000016	t	\N	\N	f	\N
27	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Insomnia	2	t	VCHR000023	\N	26	VCHR000016	t	\N	\N	f	\N
29	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Tipo Grill	2	t	VCHR000025	\N	26	VCHR000016	t	\N	\N	f	\N
21	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Tipo Grill	2	t	VCHR000017	\N	26	VCHR000016	t	\N	\N	f	\N
24	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Tipo Grill	2	t	VCHR000020	\N	26	VCHR000016	t	\N	\N	f	\N
28	1000	2025-12-11	2026-12-11	Redeemed	2025-12-20	\N	\N	Test_Approver	Phydra	2	t	VCHR000024	\N	26	VCHR000016	t	\N	\N	f	\N
208	0.0	2026-01-04	2027-01-04	Reserved	\N	\N	\N	\N	\N	\N	f	VCHR000202	\N	\N	\N	f	\N	\N	f	\N
209	0.0	2026-01-04	2027-01-04	Reserved	\N	\N	\N	\N	\N	\N	f	VCHR000203	\N	\N	\N	f	\N	\N	f	\N
212	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000206	\N	\N	\N	f	\N	\N	f	\N
213	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000207	\N	\N	\N	f	\N	\N	f	\N
214	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000208	\N	\N	\N	f	\N	\N	f	\N
215	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000209	\N	\N	\N	f	\N	\N	f	\N
216	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000210	\N	\N	\N	f	\N	\N	f	\N
217	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000211	\N	\N	\N	f	\N	\N	f	\N
218	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000212	\N	\N	\N	f	\N	\N	f	\N
219	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000213	\N	\N	\N	f	\N	\N	f	\N
211	0.0	2025-12-24	2025-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000205	\N	\N	\N	f	\N	\N	f	\N
210	0.0	2025-12-24	2026-12-31	Active	\N	\N	\N	\N	\N	\N	f	VCHR000204	\N	\N	\N	f	\N	\N	f	\N
42	500	2025-12-11	2026-12-11	Active	\N	\N	\N	\N	\N	1	f	VCHR000038	\N	46	VCHR000037	t	\N	\N	f	\N
44	100	2025-12-11	2026-12-11	Redeemed	2026-03-30	\N	\N	Bruno	Jean Paul	4	t	VCHR000040	\N	48	VCHR000040	t	\N	\N	f	\N
49	1200	2025-12-11	2026-12-11	Redeemed	2026-04-20	\N	\N	Bruno	Tipo Grill	4	t	VCHR000045	\N	53	VCHR000045	t	\N	\N	f	\N
\.


--
-- Name: audit_trail_audit_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.audit_trail_audit_id_seq', 167, true);


--
-- Name: branch_branch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.branch_branch_id_seq', 12, true);


--
-- Name: clients_ref_client_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.clients_ref_client_seq', 8, true);


--
-- Name: company_company_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.company_company_id_seq', 1, false);


--
-- Name: invoices_invoice_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.invoices_invoice_id_seq', 45, true);


--
-- Name: redemption_audit_redemption_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.redemption_audit_redemption_id_seq', 7, true);


--
-- Name: redemptions_redemption_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.redemptions_redemption_id_seq', 1, false);


--
-- Name: requests_ref_request_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.requests_ref_request_seq', 1, false);


--
-- Name: users_username_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.users_username_seq', 1, false);


--
-- Name: voucher_requests_request_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.voucher_requests_request_id_seq', 53, true);


--
-- Name: voucher_stores_voucher_store_id_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.voucher_stores_voucher_store_id_seq', 1, false);


--
-- Name: vouchers_ref_voucher_seq; Type: SEQUENCE SET; Schema: public; Owner: julienjeanpierre
--

SELECT pg_catalog.setval('public.vouchers_ref_voucher_seq', 219, true);


--
-- Name: audit_trail audit_trail_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.audit_trail
    ADD CONSTRAINT audit_trail_pkey PRIMARY KEY (audit_id);


--
-- Name: branch branch_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.branch
    ADD CONSTRAINT branch_pkey PRIMARY KEY (branch_id);


--
-- Name: clients clients_email_client_key; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_email_client_key UNIQUE (email_client);


--
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (ref_client);


--
-- Name: company company_email_company_key; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_email_company_key UNIQUE (email_company);


--
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (company_id);


--
-- Name: invoices invoices_invoice_number_key; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_invoice_number_key UNIQUE (invoice_number);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (invoice_id);


--
-- Name: redemption_audit redemption_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.redemption_audit
    ADD CONSTRAINT redemption_audit_pkey PRIMARY KEY (redemption_id);


--
-- Name: redemptions redemptions_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.redemptions
    ADD CONSTRAINT redemptions_pkey PRIMARY KEY (redemption_id);


--
-- Name: requests requests_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT requests_pkey PRIMARY KEY (ref_request);


--
-- Name: users users_email_user_key; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_user_key UNIQUE (email_user);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);


--
-- Name: voucher_requests voucher_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_requests
    ADD CONSTRAINT voucher_requests_pkey PRIMARY KEY (request_id);


--
-- Name: voucher_requests voucher_requests_request_reference_key; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_requests
    ADD CONSTRAINT voucher_requests_request_reference_key UNIQUE (request_reference);


--
-- Name: voucher_stores voucher_stores_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_stores
    ADD CONSTRAINT voucher_stores_pkey PRIMARY KEY (voucher_store_id);


--
-- Name: vouchers vouchers_pkey; Type: CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT vouchers_pkey PRIMARY KEY (ref_voucher);


--
-- Name: idx_audit_entity; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_audit_entity ON public.audit_trail USING btree (entity_type, entity_id);


--
-- Name: idx_audit_timestamp; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_audit_timestamp ON public.audit_trail USING btree ("timestamp");


--
-- Name: idx_audit_type; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_audit_type ON public.audit_trail USING btree (action_type);


--
-- Name: idx_invoices_request; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_invoices_request ON public.invoices USING btree (request_id);


--
-- Name: idx_redemption_audit_branch; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_redemption_audit_branch ON public.redemption_audit USING btree (branch);


--
-- Name: idx_redemption_audit_status; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_redemption_audit_status ON public.redemption_audit USING btree (status);


--
-- Name: idx_redemption_audit_time; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_redemption_audit_time ON public.redemption_audit USING btree (redemption_time);


--
-- Name: idx_redemptions_branch; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_redemptions_branch ON public.redemptions USING btree (branch_id);


--
-- Name: idx_redemptions_code; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_redemptions_code ON public.redemptions USING btree (voucher_code);


--
-- Name: idx_redemptions_date; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_redemptions_date ON public.redemptions USING btree (redemption_date);


--
-- Name: idx_voucher_requests_client; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_voucher_requests_client ON public.voucher_requests USING btree (ref_client);


--
-- Name: idx_voucher_requests_ref; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_voucher_requests_ref ON public.voucher_requests USING btree (request_reference);


--
-- Name: idx_voucher_stores_branch; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_voucher_stores_branch ON public.voucher_stores USING btree (branch_id);


--
-- Name: idx_voucher_stores_code; Type: INDEX; Schema: public; Owner: julienjeanpierre
--

CREATE INDEX idx_voucher_stores_code ON public.voucher_stores USING btree (voucher_code);


--
-- Name: redemption_audit trg_redemption_audit_before_insert; Type: TRIGGER; Schema: public; Owner: julienjeanpierre
--

CREATE TRIGGER trg_redemption_audit_before_insert BEFORE INSERT ON public.redemption_audit FOR EACH ROW EXECUTE FUNCTION public.normalize_redemption_audit_before_insert();


--
-- Name: requests trigger_requests_insert; Type: TRIGGER; Schema: public; Owner: julienjeanpierre
--

CREATE TRIGGER trigger_requests_insert AFTER INSERT ON public.requests FOR EACH ROW EXECUTE FUNCTION public.log_requests_insert();


--
-- Name: branch fk_branch_company; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.branch
    ADD CONSTRAINT fk_branch_company FOREIGN KEY (ref_company) REFERENCES public.company(company_id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: branch fk_branch_user; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.branch
    ADD CONSTRAINT fk_branch_user FOREIGN KEY (responsible_user) REFERENCES public.users(username) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: requests fk_request_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT fk_request_approved_by FOREIGN KEY (approved_by) REFERENCES public.users(username) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: requests fk_request_client; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT fk_request_client FOREIGN KEY (ref_client) REFERENCES public.clients(ref_client) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: requests fk_request_processed_by; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT fk_request_processed_by FOREIGN KEY (processed_by) REFERENCES public.users(username) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: requests fk_request_validated_by; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT fk_request_validated_by FOREIGN KEY (validated_by) REFERENCES public.users(username) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: vouchers fk_vouchers_client; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT fk_vouchers_client FOREIGN KEY (ref_client) REFERENCES public.clients(ref_client) ON DELETE CASCADE;


--
-- Name: vouchers fk_vouchers_request; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.vouchers
    ADD CONSTRAINT fk_vouchers_request FOREIGN KEY (ref_request) REFERENCES public.voucher_requests(request_id) ON DELETE SET NULL;


--
-- Name: invoices invoices_ref_client_fkey; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_ref_client_fkey FOREIGN KEY (ref_client) REFERENCES public.clients(ref_client) ON DELETE CASCADE;


--
-- Name: invoices invoices_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.voucher_requests(request_id) ON DELETE CASCADE;


--
-- Name: redemptions redemptions_branch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.redemptions
    ADD CONSTRAINT redemptions_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branch(branch_id) ON DELETE SET NULL;


--
-- Name: redemptions redemptions_voucher_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.redemptions
    ADD CONSTRAINT redemptions_voucher_code_fkey FOREIGN KEY (voucher_code) REFERENCES public.vouchers(ref_voucher) ON DELETE CASCADE;


--
-- Name: voucher_requests voucher_requests_ref_client_fkey; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_requests
    ADD CONSTRAINT voucher_requests_ref_client_fkey FOREIGN KEY (ref_client) REFERENCES public.clients(ref_client) ON DELETE CASCADE;


--
-- Name: voucher_stores voucher_stores_branch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: julienjeanpierre
--

ALTER TABLE ONLY public.voucher_stores
    ADD CONSTRAINT voucher_stores_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branch(branch_id) ON DELETE SET NULL;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: julienjeanpierre
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict YuDS5YEYwu3EOlLHYc4UMPfjdmffYl05d5fyJsgF1ow4T8vUEkWTL7qIHgg5HAY

