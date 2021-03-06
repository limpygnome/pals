Global Hooks
============
Global hooks are used for handling system-wide events, apart of the plugin manager
event mechanism.

Default Hooks
-------------
	Event:	base.nodes.update
	Args:	(none)
	Desc:	Invoked to indicate the list of nodes should be updated.
	
	Event:	base.web.request_start
	Args:	WebRequestData
	Desc:	Invoked at the start of a web-request.

	Event:	base.web.request_end
	Args:	WebRequestData
	Desc:	Invoked at the end of a web-request.

	Event:	base.web.request_404
	Args:	WebRequestData
	Desc:	Invoked when a page cannot be handled and a HTTP 404 event should be handled.

	Event:	base.assessment.wake
	Args:	(none)
	Desc:	Informs any background services that work is available for marking.

	Event:	base.web.email.wake
	Args:	(none)
	Desc:	Informs any background services that e-mail is ready for sending.

	Event:	base.cleaner.wake
	Args:	(none)
	Desc:	Informs any background services that data is pending a clean-up from the pals_cleanup
			table.

	Event:	question_type.web_edit
	Args:	WebRequestData
		Question
	Desc:	Invoked for a question-handler to handle a web-request for editing a type of question it
		handles.

	Event:	question_type.question_capture
	Args:	WebRequestData
			InstanceAssignment
			AssignmentQuestion
			InstanceAssignmentQuestion
			StringBuilder
			Boolean
	Desc:	Invoked for a question-handler to render the HTML for a question instance for an instance of
			an assignment, when capturing input from a user.

	Event:	question_type.question_display
	Args:	WebRequestData
			InstanceAssignment
			AssignmentQuestion
			InstanceAssignmentQuestion
			StringBuilder
			Boolean - secure
			Boolean - edit-mode
	Desc:	Invoked for a question-handler to render the HTML for a question instance for an instance of
			an assignment, when displaying the result from input (after submission of instance).

	Event:	criteria_type.web_edit
	Args:	WebRequestData
			QuestionCriteria
	Desc:	Invoked for a criteria-handler to handle a web-request for editing a type of criteria it
			handles.

	Event:	criteria_type.mark
	Args:	Connector
			NodeCore
			InstanceAssignmentCriteria
	Desc:	Invoked for a criteria-handler to mark a criteria it handles, for an instance of an
			assignment.

	Event:	criteria_type.display_feedback
	Args:	WebRequestData
			InstanceAssignment
			InstanceAssignmentQuestion
			InstanceAssignmentCriteria
			StringBuilder
	Desc:	Invoked for a criteria-handler to render the HTML for displaying feedback of a marked
			criteria.

