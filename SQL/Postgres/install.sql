-- The plugins which compromise of the system; these are responsible for assessment and handling web-pages.
CREATE TABLE pals_plugins
(
	plugin_uuid			BYTEA(16)			PRIMARY KEY,
	title				VARCHAR(64),
	system				VARCHAR(1) 			DEFAULT 0			-- Indicates the plugin is a system-type (boolean); if so, it should not be removable.
);
-- Collection of templates shared by all plugins for rendering; these are loaded and cached by each node.
CREATE TABLE pals_templates
(
	path				VARCHAR(64)			PRIMARY KEY,
	content				TEXT,
	plugin_uuid			BYTEA(16)			REFERENCES `pals_plugins`(`plugin_uuid`) ON UPDATE CASCADE ON DELETE CASCADE
);

-- The users on the system; this does not include authentication, this is handled else-where; thus multiple authentication systems can use the same
-- user table indepently for the same user.
CREATE TABLE pals_users
(
	userid				SERIAL				PRIMARY KEY,
	alias				VARCHAR(64)			-- Note: this is not a username; this is just used to display an 'alias' for the user.
	email				VARCHAR(128)
);
-- Possible modules for student enrollment.
CREATE TABLE pals_modules
(
	moduleid			SERIAL				PRIMARY KEY,
);
CREATE TABLE pals_modules_enrollment
(
	moduleid,
	userid,
	UNIQUE(moduleid, userid)
);



-- Possible types of questions; linked to plugins for rendering.
CREATE TABLE pals_question_types
(
	qtype_uuid			BYTEA(16)			PRIMARY KEY
	plugin_uuid			BYTEA(16)			REFERENCES `pals_plugins`(`plugin_uuid`) ON UPDATE CASCADE ON DELETE NO ACTION			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible types of criteria handlers for assessing work; linked to plugins for handling assessment of work.
CREATE TABLE pals_criteria_types
(
	ctype_uuid			BYTEA(16)			PRIMARY KEY,
	plugin_uuid			BYTEA(16)			REFERENCES `pals_plugins`(`plugin_uuid`) ON UPDATE CASCADE ON DELETE NO ACTION 			NOT NULL,
	title				VARCHAR(64),
	description			TEXT
);
-- Possible criteria types for each question type; a single criteria may handle multiple question types.
CREATE TABLE pals_qtype_ctype
(
	qtype_uuid			BYTEA(16)			REFERENCES `pals_question_types`(`qtype_uuid`) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	ctype_uuid			BYTEA(16)			REFERENCES `pals_criteria_types`(`ctype_uuid`) ON UPDATE CASCADE ON DELETE CASCADE		NOT NULL,
	UNIQUE(qtype_uuid, ctype_uuid)
);
-- Possible questions, independent of assignments; this is so questions can be re-used across different assignments.
CREATE TABLE pals_question
(
	qid					SERIAL				PRIMARY KEY,
	qtype_uuid			BYTEA(16)			REFERENCES `pals_question_types`(`qtype_uuid`) ON UPDATE CASCADE ON DELETE RESTRICT		NOT NULL
	title				VARCHAR(64)
);
-- Criteria for assessing a question; this will be responsible for assigning marks.
CREATE TABLE pals_question_criteria
(
	qid					INT					REFERENCES `pals_question`(`qid`) ON UPDATE CASCADE ON DELETE CASCADE,
	ctype_uuid			BYTEA(16)			REFERENCES `pals_criteria_types`(`ctype`) ON UPDATE CASCADE ON DELETE CASCADE,
	params				TEXT,		-- Any unique parameter data to be passeed to the criteria handler
	UNIQUE(qid, ctype_uuid)
	-- ctype qtype must be in qtype-ctype! mysql may have an issue with checking this.
);
-- Possible
CREATE TABLE pals_assignments
(
	assid				SERIAL				PRIMARY KEY,
	moduleid			INT					REFERENCES `pals_modules`(`moduleid`) ON UPDATE CASCADE ON DELETE CASCADE,
	
);

CREATE TABLE pals_assignments_questions
(
	aqid,
	assignmentid,
	questionid,
	weight,
	question,
	group
);


CREATE TABLE pals_assignments_questions_instance
(
	aqid,
	-- can be displayed in edit or view mode.
);
CREATE TABLE pals_assignments_questions_instance_data
(
	-- kv data for a question instance
	-- ability to store e.g. code, anything.
);