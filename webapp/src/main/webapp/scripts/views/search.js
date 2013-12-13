var itemColor = 1;
//Initialize your view
view.content = '';

view.init = function() {
	autocomplete('searchField');	
	$("#searchForm").on("submit", function(e){
		getSearch($('#searchField').val(), 'document', 20);
		return false;
	});
	//this.content = $('<div>').attr('class', 'documentList').html(template());
	
	//assign documentModel to GUI
	//ko.applyBindings(documentModel, this.content[0]);
};
/*
function template()
{
	var template = '<ul data-bind="foreach: documentData">'+
	'<!-- ko if: hasOwnProperty("id") -->'+
	'<li class="documents" data-bind="attr: { \'id\': \'doc_\'+$data.id }">'+
		'<div class="docButtons">'+
			'<button class="addButton" type="button" title="add to shortlist" name="addToCart"></button>'+
			'<button class="chartButton" type="button" title="show topic mixture" name="showBig" onclick=""></button>'+
		'</div>'+
		'<a href="" class="docTitle"><nobr data-bind="text: name"></nobr></a>'+
		'<p class="docContent"><span data-bind="text: textSnippet"></span></p>'+
		'<div class="circles" data-bind="drawCircles: topTopics">'+
			'<svg width="100%" height="100%"></svg>'+
		'</div>'+
	'</li>'+
	'<!-- /ko -->'+
	'</ul>';
	return template;
}
*/
function autocomplete(boxID) {

	$('#' + boxID).bind('keydown', function() {
		$('#searchItem').val("none");
	});
	$('#' + boxID).bind('keyup', function() {
		autocompleteSearch = $('#' + boxID).val();
	});
	$("#" + boxID).autocomplete( {
		source : function(request, response) {
			$.ajax( {
				url : "JsonServlet",
				dataType : "json",
				cache : true,
				data : {
					Command: 'autocomplete',
					SearchWord : request.term
				},
				type : 'GET',
				success : function(data) {
					response($.map(data, function(item) {
						return {
							label : item.TERM_NAME,
							color1 : item.TOP_TOPIC[0],
							color2 : item.TOP_TOPIC[1],
							color3 : item.TOP_TOPIC[2],
							color4 : item.TOP_TOPIC[3],
							value : item.label,
							item : 'document'
						};
					}));
				}
			});
		},
		select : function(event, ui) {
			$('#searchField').autocomplete("close");
			getSearch(ui.item.label, ui.item.item, 20);
		},
		minLength : 1,
		delay : 700
	}).data("ui-autocomplete")._renderItem = function(ul, item) {
		console.log(item);
		var circleString = "<a onmouseover=\"$('#" + boxID + "').val('"
				+ item.label + "')\"  onmouseout=\"$('#" + boxID
				+ "').val('')\" onclick=\"$('#" + boxID
				+ "').parent('form').submit()\"><img src=\"images/" + item.item
				+ ".png\" title=\"" + item.item + "\" />" + item.label
				+ "<svg width=\"55px\" height=\"20px\">";
		if (item.color1 > -1)
			circleString += generateCircle(item.color1);
		if (item.color2 > -1)
			circleString += generateCircle(item.color2);
		if (item.color3 > -1)
			circleString += generateCircle(item.color3);
		if (item.color4 > -1)
			circleString += generateCircle(item.color4);

		circleString += "</svg></a>";
		ul.find('svg').delegate("circle", "click", move);
		return $("<li></li>").data("item.autocomplete", item).append(
				circleString).appendTo(ul);
	};
}

function getSearch(searchWord, item, limit) {
	alert(searchWord + ", " + item + ", " + limit);	
	$.getJSON('JsonServlet', {Command:'search', SearchWord:searchWord})
	.done(function(json) {
		topicExplorer.jsonModel.DOCUMENT = ko.mapping.fromJS(json.DOCUMENT);
	});
	var textModel = topicExplorer.viewModel.getView('browser').getDocumentViewModel("topicRelevance", 0);
	var template = topicExplorer.viewModel.getView('browser').getTemplate();//(topicExplorer.jsonModel);
	ko.applyBindings(textModel, template[0]);
	gui.drawTab("Search: " + searchWord, true, true, template);
}

function move(e) {
	moveToTopic(e);
	e.preventDefault();
	return false;
}

function generateCircle(color) {
	var cx = 10 + itemColor * 12;
	var topic = topicModel.topicList()[color];
	if(!topic)
		return "";
	var circleString = "<circle class=\"topic_"+color+"\" onmouseover=\"$(this).attr('r','7');\" onmouseout=\"$(this).attr('r','5');\" "
		+ " r=\"5\" cx=\""+cx+"\" cy=\"14\" fill=\""
		+ topic.topicColor
		+ "\" title=\""
		+ topic.topicTitle[0]
		+ "\" stroke=\"black\" stroke-width=\"0.5\" style=\"cursor:pointer\"/>";
	itemColor++;
	return circleString;
}