-- The nodes used for the assessment of work and other tasks.
CREATE TABLE pals_nodes
(
	-- The UUID identifier of the node.
	uuid_node			BYTEA				PRIMARY KEY,
	-- The title of the node; optional.
	title				VARCHAR(64)			DEFAULT 'Untitled Node',
	-- The date/time of when the node was last active.
	last_active			TIMESTAMP			NOT NULL,
	-- RMI information - updated everytime a node starts-up.
	rmi_ip				VARCHAR(45),
	rmi_port			INT
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
-- Used to assign permissions to different classes of users.
CREATE TABLE pals_users_group
(
	groupid				SERIAL				PRIMARY KEY,
	title				VARCHAR(64)			DEFAULT 'Untitled Group',
	-- User permissions
	-- -- Login
	user_login			VARCHAR(1)			DEFAULT 0,
	
	-- Marker permissions
	-- -- Marking in general
	marker_general		VARCHAR(1)			DEFAULT 0,
	
	-- Admin permissions
	-- -- Manage modules.
	admin_modules		VARCHAR(1)			DEFAULT 0,
	-- -- Manage assignments for modules.
	admin_assignments	VARCHAR(1)			DEFAULT 0,
	-- -- Manage users.
	admin_users			VARCHAR(1)			DEFAULT 0,
	-- -- Manage the system (nodes/logs/plugins/etc).
	admin_system		VARCHAR(1)			DEFAULT 0
);
-- -- Add default user-groups
INSERT INTO pals_users_group (title,user_login,marker_general,admin_modules,admin_assignments,admin_users,admin_system) VALUES
('Admins','1','1','1','1','1','1'),
('Users','0','0','0','0','0','0')
;
-- Represents abstract users on the system. Columns password and password_salt columns are optional,
-- used for default authentication.
CREATE TABLE pals_users
(
	userid				SERIAL				PRIMARY KEY,
	-- The username for the user; displayed as an alias and used for logging-in with default authentication.
	username			VARCHAR(24)			NOT NULL,
	-- Optional; the password for the user, used for authentication; this stores the hash.
	password			TEXT,
	-- Optional; the unique salt for the user.
	password_salt		VARCHAR(32),
	-- Optional; allows the system to e-mail users.
	email				VARCHAR(128),
	-- The user-group of the user.
	groupid				INT					REFERENCES pals_users_group(groupid) ON UPDATE CASCADE ON DELETE CASCADE NOT NULL
);
-- -- Ensure all usernames and e-mails are unique, regardless of case.
-- -- -- Index on usernames is also needed to speed-up account searches.
CREATE UNIQUE INDEX index_pals_users_username ON pals_users (lower(username));
CREATE UNIQUE INDEX index_pals_users_email ON pals_users (lower(email));

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
	title				VARCHAR(64)			DEFAULT 'Untitled Module'		NOT NULL
);
-- Modules of which a student is enrolled-upon
CREATE TABLE pals_modules_enrollment
(
	moduleid			INT	REFERENCES pals_modules(moduleid)	ON UPDATE CASCADE ON DELETE CASCADE NOT NULL,
	userid				INT	REFERENCES pals_users(userid)		ON UPDATE CASCADE ON DELETE CASCADE NOT NULL,
	PRIMARY KEY(moduleid, userid)
);



-- Possible types of questions; linked to plugins which handle them (text, multiple-choice, etc).
CREATE TABLE pals_question_types
(
	uuid_qtype			BYTEA				PRIMARY KEY,
	uuid_plugin			BYTEA				REFERENCES pals_plugins(uuid_plugin) ON UPDATE CASCADE ON DELETE NO ACTION			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible types of criteria handlers for assessing work; linked to plugins for handling assessment of work.
-- -- Allows plugins to be created for makring different types of questions in different ways.
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
	title				VARCHAR(64)			DEFAULT 'Untitled Question'		NOT NULL,
	data				BYTEA
);
-- Criteria for assessing a question; this will be responsible for assigning marks. Any criteria parameters are stored and handled by the criteria-type.
CREATE TABLE pals_question_criteria
(
	qcid				SERIAL				PRIMARY KEY,
	qid					INT					REFERENCES pals_question(qid) 				ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	uuid_ctype			BYTEA				REFERENCES pals_criteria_types(uuid_ctype) 	ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	title				VARCHAR(64)			DEFAULT 'Untitled Criteria'					NOT NULL,
	data				BYTEA,
	weight				INT					NOT NULL
);
-- -- Index on QID since we'll often load criterias by the identifier of a question.
CREATE INDEX index_pals_question_criteria_qid ON pals_question_criteria (qid);


-- An assignment, many to a single module.
CREATE TABLE pals_assignment
(
	assid				SERIAL				PRIMARY KEY,
	moduleid			INT					REFERENCES pals_modules(moduleid) ON UPDATE CASCADE ON DELETE CASCADE,
	title				VARCHAR(64)			DEFAULT 'Untitled Assignment' NOT NULL,
	-- The weight of the assignment; similiar to weight in pals_assignment_questions.
	weight				INT					NOT NULL,
	-- Indicates if the assignment is active (1) or inactive/disabled (0).
	active				VARCHAR(1)			DEFAULT 0
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
	page_order			INT					DEFAULT 0																			NOT NULL
);



-- An instance of an assignment, created when a user attempts an assignment.
CREATE TABLE pals_assignment_instance
(
	aiid				SERIAL				PRIMARY KEY,
	-- The user who has answered the assignment.
	userid				INT					REFERENCES pals_users(userid) ON UPDATE CASCADE ON DELETE CASCADE					NOT NULL,
	-- The assignment to which the instance belongs.
	assid				INT					REFERENCES pals_assignment(assid) ON UPDATE CASCADE ON DELETE CASCADE				NOT NULL,
	-- The status of the assignment, as controlled by 'pals.base.assessment.InstanceAssignment.Status'.
	status				INT					DEFAULT 0,
	-- The mark achieved for the assignment; calculated when an assignment has been completely marked.
	-- -- Acts as a cache to avoid expensive aggregate functions.
	-- -- 0 to 100.
	mark				DOUBLE PRECISION	DEFAULT 0																			NOT NULL
);
-- Data for answered questions of an instance of an assignment; this table may not be used by plugins handling instance data on their own.
CREATE TABLE pals_assignment_instance_question
(
	aiqid				SERIAL				PRIMARY KEY,
	-- The assignment question.
	aqid				INT					REFERENCES pals_assignment_questions(aqid) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	-- The instance of the assignment.
	aiid				INT					REFERENCES pals_assignment_instance(aiid) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	-- Data used to answer the question; defined by question-type handler, used by criteria-type handlers for marking.
	data				BYTEA,
	-- Indicates if the question has been answered.
	answered			VARCHAR(1)			DEFAULT '0'																			NOT NULL,
	-- Ensure aqid and aiid are unique; also creates indexes for fast reverse querying.
	UNIQUE(aqid, aiid)
);
-- -- Create indexes on the columns used for reverse lookups of questions.
CREATE INDEX index_pals_assignment_instance_question_aqid ON pals_assignment_instance_question(aqid);
CREATE INDEX index_pals_assignment_instance_question_aiid ON pals_assignment_instance_question(aiid);

-- Used to allocate marks to specific criterias for an instance of an assignment's question.
-- -- Created during marking.
CREATE TABLE pals_assignment_instance_question_criteria
(
	-- The instance of the question.
	aiqid				INT					REFERENCES pals_assignment_instance_question(aiqid) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	-- The question criteria.
	qcid				INT					REFERENCES pals_question_criteria(qcid) ON UPDATE CASCADE ON DELETE CASCADE				NOT NULL,
	-- The status of marking the criteria; refer to pals.base.assessment.InstanceAssignmentCriteria.Status for values.
	status				INT					DEFAULT 0																				NOT NULL,
	-- The mark allocated to the criteria; 0 to 100.
	mark				INT					DEFAULT 0																				NOT NULL,
	-- The time at which the criteria was last processed; if this period exceeds a certain limit, as controlled by the nodes,
	-- the work is available for re-processing.
	last_processed		TIMESTAMP,
	PRIMARY KEY(aiqid, qcid)
);


