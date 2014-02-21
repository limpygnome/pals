DROP TABLE IF EXISTS pals_nodes									CASCADE;
DROP TABLE IF EXISTS pals_plugins								CASCADE;
DROP TABLE IF EXISTS pals_http_sessions							CASCADE;
DROP TABLE IF EXISTS pals_http_session_data						CASCADE;
DROP TABLE IF EXISTS pals_users_group							CASCADE;
DROP TABLE IF EXISTS pals_users									CASCADE;

DROP TABLE IF EXISTS pals_modules								CASCADE;
DROP TABLE IF EXISTS pals_modules_enrollment					CASCADE;

DROP TABLE IF EXISTS pals_question_types						CASCADE;
DROP TABLE IF EXISTS pals_criteria_types						CASCADE;
DROP TABLE IF EXISTS pals_qtype_ctype							CASCADE;

DROP TABLE IF EXISTS pals_question								CASCADE;
DROP TABLE IF EXISTS pals_question_criteria						CASCADE;

DROP TABLE IF EXISTS pals_assignment							CASCADE;
DROP TABLE IF EXISTS pals_assignment_questions					CASCADE;

DROP TABLE IF EXISTS pals_assignment_instance 					CASCADE;
DROP TABLE IF EXISTS pals_assignment_instance_question 			CASCADE;
DROP TABLE IF EXISTS pals_assignment_instance_question_criteria	CASCADE;

DROP TABLE IF EXISTS pals_node_locking							CASCADE;

DROP TABLE IF EXISTS pals_exception_classes						CASCADE;
DROP TABLE IF EXISTS pals_exception_messages					CASCADE;
DROP TABLE IF EXISTS pals_exceptions							CASCADE;
