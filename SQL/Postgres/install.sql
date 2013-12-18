-- The nodes used for the assessment of work and other tasks.
CREATE TABLE pals_nodes
(
	-- The UUID identifier of the node.
	uuid_node			BYTEA,
	-- The title of the node; optional.
	title				VARCHAR(64)			DEFAULT 'Untitled Node',
	-- The date/time of when the node was last active.
	last_active			TIMESTAMP			NOT NULL
);
-- The plugins which compromise of the system; these are responsible for assessment, handling web-pages
-- and other system tasks.
CREATE TABLE pals_plugins
(
	-- The UUID identifier of the plugin.
	uuid_plugin			BYTEA				PRIMARY KEY,
	-- The title of the plugin.
	title				VARCHAR(64),
	-- The state of the plugin (refer to pals.base.Plugin.DbPluginState).
	state				INT					DEFAULT 0,
	-- Indicates the plugin is a system-type (boolean); if so, it should not be removable.
	system				VARCHAR(1) 			DEFAULT 0
);
-- Instances of HTTP sessions, used to store data between requests of web-users.
CREATE TABLE pals_http_sessions
(
	sessid				BYTEA				PRIMARY KEY,
	creation			TIMESTAMP			NOT NULL,
	last_active			TIMESTAMP			NOT NULL,
	ip					VARCHAR(45)			NOT NULL
);
-- Stores data belonging to a HTTP session.
CREATE TABLE pals_http_session_data
(
	sessid				BYTEA				REFERENCES pals_http_sessions(sessid) ON UPDATE CASCADE ON DELETE CASCADE,
	key					VARCHAR(32),
	data				BYTEA,
	PRIMARY KEY(sessid, key)
);
-- Represents abstract users on the system. Columns password and password_salt columns are optional,
-- used for default authentication.
CREATE TABLE pals_users
(
	userid				SERIAL				PRIMARY KEY,
	-- The username for the user; displayed as an alias and used for logging-in with default authentication.
	username			VARCHAR(64)			NOT NULL,
	-- Optional; the password for the user, used for authentication; this stores the hash.
	password			VARCHAR(128),
	-- Optional; the unique salt for the user.
	password_salt		VARCHAR(32),
	-- Optional; allows the system to e-mail users.
	email				VARCHAR(128)
);
CREATE INDEX index_pals_users_username ON pals_users (username);
-- The e-mail queue; used to avoid loss of possible e-mails from the system rebooting. Also
-- allows multiple nodes to process emails.
CREATE TABLE pals_email_queue
(
	emailid				SERIAL				PRIMARY KEY,
	title				VARCHAR(128)		NOT NULL,
	content				TEXT				NOT NULL,
	-- The e-mail length is based on http://www.rfc-editor.org/errata_search.php?rfc=3696&eid=1690
	destination			VARCHAR(254)		NOT NULL,
	last_attempted		TIMESTAMP
);

-- Possible modules for student enrollment.
CREATE TABLE pals_modules
(
	moduleid			SERIAL				PRIMARY KEY,
	title				VARCHAR(64)			DEFAULT 'Untitled Module'
);
-- Modules of which a student is enrolled-upon
CREATE TABLE pals_modules_enrollment
(
	moduleid			INT	REFERENCES pals_modules(moduleid)	ON UPDATE CASCADE ON DELETE CASCADE NOT NULL,
	userid				INT	REFERENCES pals_users(userid)		ON UPDATE CASCADE ON DELETE CASCADE NOT NULL,
	PRIMARY KEY(moduleid, userid)
);



-- Possible types of questions; linked to plugins which handle them.
CREATE TABLE pals_question_types
(
	uuid_qtype			BYTEA				PRIMARY KEY,
	uuid_plugin			BYTEA				REFERENCES pals_plugins(uuid_plugin) ON UPDATE CASCADE ON DELETE NO ACTION			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible types of criteria handlers for assessing work; linked to plugins for handling assessment of work.
CREATE TABLE pals_criteria_types
(
	uuid_ctype			BYTEA				PRIMARY KEY,
	uuid_plugin			BYTEA				REFERENCES pals_plugins(uuid_plugin) ON UPDATE CASCADE ON DELETE NO ACTION 			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible criteria types for each question type; a single criteria may handle multiple question types.
CREATE TABLE pals_qtype_ctype
(
	uuid_qtype			BYTEA				REFERENCES pals_question_types(uuid_qtype) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	uuid_ctype			BYTEA				REFERENCES pals_criteria_types(uuid_ctype) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	PRIMARY KEY(uuid_qtype, uuid_ctype)
);



-- Possible questions, independent of assignments; this is so questions can be re-used across different assignments.
CREATE TABLE pals_question
(
	qid					SERIAL				PRIMARY KEY,
	uuid_qtype			BYTEA				REFERENCES pals_question_types(uuid_qtype) ON UPDATE CASCADE ON DELETE RESTRICT		NOT NULL,
	title				VARCHAR(64)
);
-- Criteria for assessing a question; this will be responsible for assigning marks. Any criteria parameters are stored and handled by the criteria-type.
CREATE TABLE pals_question_criteria
(
	qid					INT					REFERENCES pals_question(qid) 				ON UPDATE CASCADE ON DELETE CASCADE,
	uuid_ctype			BYTEA				REFERENCES pals_criteria_types(uuid_ctype) 	ON UPDATE CASCADE ON DELETE CASCADE,
	PRIMARY KEY(qid, uuid_ctype)
);



-- An assignment, many to a single module.
CREATE TABLE pals_assignment
(
	assid				SERIAL				PRIMARY KEY,
	moduleid			INT					REFERENCES pals_modules(moduleid) ON UPDATE CASCADE ON DELETE CASCADE,
	title				VARCHAR(64)			DEFAULT 'Untitled Assignment',
	-- The weight of the assignment; similiar to weight in pals_assignment_questions.
	weight				INT																										NOT NULL
);
-- The questions which belong to an assignment; note: the same question may be used multiple times.
CREATE TABLE pals_assignment_questions
(
	aqid				SERIAL				PRIMARY KEY,
	assid				INT					REFERENCES pals_assignment(assid) 	ON UPDATE CASCADE ON DELETE CASCADE				NOT NULL,
	-- The identifier of the question being used for the assignment question; allows multiple of the same questions.
	qid					INT					REFERENCES pals_question(qid) 		ON UPDATE CASCADE ON DELETE RESTRICT			NOT NULL,
	-- The weight of the question in the assignment; the total weight is summed for all questions to form the maximum available mark. Therefore the percent of this
	-- question is weight/total_weight.
	weight				INT																										NOT NULL,
	-- Questions can be put into pages; questions are then displayed ascending. Questions belonging to the same group will also appear on the same page; this
	-- field is used purely for dislay purposes.
	page				INT																										NOT NULL,
	-- The order of a question on a page.
	zindex				INT					DEFAULT 0																			NOT NULL
);



-- An instance of an assignment, created when a user attempts an assignment.
CREATE TABLE pals_assignment_instance
(
	aiid				SERIAL				PRIMARY KEY,
	-- The user who has answered the assignment.
	userid				INT					REFERENCES pals_users(userid) ON UPDATE CASCADE ON DELETE CASCADE					NOT NULL,
	-- The status of the assignment.
	status				INT					DEFAULT 0,
	-- The mark achieved for the assignment; this is cached to avoid SUM queries.
	mark				INT					DEFAULT 0
);
-- Data for answered questions of an instance of an assignment; this table may not be used by plugins handling instance data on their own.
CREATE TABLE pals_assignment_instance_data
(
	aqid				INT					REFERENCES pals_assignment_questions(aqid) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	-- Contains data used to answer the question; this can be key/value or even null.
	data				TEXT,
	-- The status of the question (answered/marking error/etc).
	status				INT					DEFAULT 0,
	-- The mark achieved for this question; this is set after the question has been marked.
	mark				INT					DEFAULT 0
);
