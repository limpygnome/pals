-- The plugins which compromise of the system; these are responsible for assessment and handling web-pages.
CREATE TABLE `pals_plugins`
(
	-- The UUID identifier of the plugin; only one instance of a plugin should ever run and a UUID will allow
	-- inter-plugin communiation.
	uuid_plugin			BYTEA				PRIMARY KEY,
	-- The title of the plugin.
	title				VARCHAR(64),
	-- Indicates the plugin is a system-type (boolean); if so, it should not be removable.
	system				VARCHAR(1) 			DEFAULT 0,
	-- The class-path of the plugin.
	classpath			VARCHAR(256)
);
-- The users on the system; this does not include authentication, this is handled else-where; thus multiple authentication systems can use the same
-- user table indepently for the same user.
CREATE TABLE `pals_users`
(
	userid				SERIAL				PRIMARY KEY,
	-- The username for the user, used for logging-in.
	username			VARCHAR(64)			NOT NULL,
	-- The password for the user, used for authentication; this stores the hash.
	password			VARCHAR(128)		NOT NULL,
	-- The unique salt for the user.
	password_salt		VARCHAR(32)			NOT NULL,
	-- Optional; allows the system to e-mail users.
	email				VARCHAR(128)
);
-- The nodes used for the assessment of work and other tasks.
CREATE TABLE `pals_nodes`
(
	node_uuid			BYTEA,
	title				VARCHAR(64)			DEFAULT 'Untitled Node',
	-- 1 or 0 (boolean) ~ indicates if the node is a master
	master				VARCHAR(1)			DEFAULT 0,
	-- The IP address of the node; 45 characters to allow for IPv6
	ip_address			VARCHAR(45)			NOT NULL,
	-- Port of the node
	port				INT
);
-- The e-mail queue; used to avoid loss of possible e-mails from the system rebooting.
CREATE TABLE `pals_email_queue`
(
	emailid				SERIAL				PRIMARY KEY,
	title				VARCHAR(128)		NOT NULL,
	content				TEXT				NOT NULL,
	-- The e-mail length is based on http://www.rfc-editor.org/errata_search.php?rfc=3696&eid=1690
	destination			VARCHAR(254)		NOT NULL,
	last_attempted		TIMESTAMP
);
-- Used for delegating paths to plugins.
CREATE TABLE `pals_urlrewriting`
(
	-- The relative path; doesn't start or end with a slash.
	path				VARCHAR(128)		NOT NULL,
	-- The UUID of the plugin which owns the path.
	uuid_plugin			BYTEA				REFERENCES `pals_plugins`(`uuid_plugin`) ON UPDATE CASCADE ON DELETE CASCADE,
	-- The priority of the path; the paths with the highest priority are served first.
	priority			INT					DEFAULT 0,
	-- Allows for the same path to be shared by multiple plugins.
	PRIMARY KEY(path, uuid_plugin);
);



-- Possible modules for student enrollment.
CREATE TABLE `pals_modules`
(
	moduleid			SERIAL				PRIMARY KEY,
	title				VARCHAR(64)			DEFAULT 'Untitled Module'
);
-- Modules of which a student is enrolled-upon
CREATE TABLE `pals_modules_enrollment`
(
	moduleid,
	userid,
	UNIQUE(moduleid, userid)
);



-- Possible types of questions; linked to plugins which handle them.
CREATE TABLE `pals_question_types`
(
	qtype_uuid			BYTEA(16)			PRIMARY KEY
	uuid_plugin			BYTEA(16)			REFERENCES `pals_plugins`(`uuid_plugin`) ON UPDATE CASCADE ON DELETE NO ACTION			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible types of criteria handlers for assessing work; linked to plugins for handling assessment of work.
CREATE TABLE `pals_criteria_types`
(
	ctype_uuid			BYTEA(16)			PRIMARY KEY,
	uuid_plugin			BYTEA(16)			REFERENCES `pals_plugins`(`uuid_plugin`) ON UPDATE CASCADE ON DELETE NO ACTION 			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible criteria types for each question type; a single criteria may handle multiple question types.
CREATE TABLE `pals_qtype_ctype`
(
	qtype_uuid			BYTEA(16)			REFERENCES `pals_question_types`(`qtype_uuid`) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	ctype_uuid			BYTEA(16)			REFERENCES `pals_criteria_types`(`ctype_uuid`) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	UNIQUE(qtype_uuid, ctype_uuid)
);




-- Possible questions, independent of assignments; this is so questions can be re-used across different assignments.
CREATE TABLE `pals_question`
(
	qid					SERIAL				PRIMARY KEY,
	qtype_uuid			BYTEA(16)			REFERENCES `pals_question_types`(`qtype_uuid`) ON UPDATE CASCADE ON DELETE RESTRICT		NOT NULL
	title				VARCHAR(64)
);
-- Criteria for assessing a question; this will be responsible for assigning marks. Any criteria parameters are stored and handled by the criteria-type.
CREATE TABLE `pals_question_criteria`
(
	qid					INT					REFERENCES `pals_question`(`qid`) 			ON UPDATE CASCADE ON DELETE CASCADE,
	ctype_uuid			BYTEA(16)			REFERENCES `pals_criteria_types`(`ctype`) 	ON UPDATE CASCADE ON DELETE CASCADE,
	UNIQUE(qid, ctype_uuid)
	-- ctype qtype must be in qtype-ctype! mysql may have an issue with checking this.
);



-- An assignment, many to a single module.
CREATE TABLE `pals_assignment`
(
	assid				SERIAL				PRIMARY KEY,
	moduleid			INT					REFERENCES `pals_modules`(`moduleid`) ON UPDATE CASCADE ON DELETE CASCADE,
	title				VARCHAR(64)			DEFAULT 'Untitled Assignment',
	-- The weight of the assignment; similiar to weight in pals_assignment_questions.
	weight				INT																											NOT NULL
);
-- The questions which belong to an assignment; note: the same question may be used multiple times.
CREATE TABLE `pals_assignment_questions`
(
	aqid				SERIAL				PRIMARY KEY,
	assid				INT					REFERENCES `pals_assignments`(`assid`) 	ON UPDATE CASCADE ON DELETE CASCADE				NOT NULL,
	-- The identifier of the question being used for the assignment question; allows multiple of the same questions.
	qid					INT					REFERENCES `pals_question`(`qid`) 		ON UPDATE CASCADE ON DELETE RESTRICT			NOT NULL
	-- The weight of the question in the assignment; the total weight is summed for all questions to form the maximum available mark. Therefore the percent of this
	-- question is weight/total_weight.
	weight				INT																											NOT NULL,
	-- Questions can be put into numeric groups; questions are then displayed ascending. Questions belonging to the same group will also appear on the same page; this
	-- field is used purely for dislay purposes.
	group				INT																											NOT NULL
);



-- An instance of an assignment, created when a user attempts an assignment.
CREATE TABLE `pals_assignment_instance`
(
	aiid				SERIAL				PRIMARY KEY,
	-- The user who has answered the assignment.
	userid				INT					REFERENCES `pals_users`(`userid`) ON UPDATE CASCADE ON DELETE CASCADE					NOT NULL,
	-- The status of the assignment.
	status				INT					DEFAULT 0,
	-- The mark achieved for the assignment; this is cached to avoid SUM queries.
	mark				INT					DEFAULT 0
);
-- Data for answered questions of an instance of an assignment; this table may not be used by plugins handling instance data on their own.
CREATE TABLE `pals_assignment_instance_data`
(
	aqid				INT					REFERENCES `pals_assignments_questions`(`aqid`) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	-- Contains data used to answer the question; this can be key/value or even null.
	data				TEXT,
	-- The status of the question (answered/marking error/etc).
	status				INT					DEFAULT 0,
	-- The mark achieved for this question; this is set after the question has been marked.
	mark				INT					DEFAULT 0
);
