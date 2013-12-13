//Initialize your view
view.setTab = true;
view.setActive = true;
view.tabName = 'Dokumentenview';
view.view = '';
view.content = '';
view.text = ko.observableArray();


view.init = function() {
	this.content = $('<div>').attr('class', 'documentList').html(view.Template());
	
	//use delegate, cause content is not loaded yet	
	//bind on desktop, cause this element is not refreshed by tab view and don't lose bindings 
	$('#desktop').delegate(".documentList li", 'mouseover', function(e){
		$(this).addClass('myHover').children(".docButtons").show();
	})
	.delegate(".documentList li", 'mouseout', function(e){
		$(this).removeClass('myHover').children(".docButtons").hide();
	})	
	.delegate(".documentList circle", "mouseover",function(){
		$(this).attr("r", "7");
	})
	.delegate(".documentList circle", "mouseout",function(){
		$(this).attr("r", "5");
	})
	.delegate(".documentList circle", "click", moveToTopic)
	.delegate(".documentList .docTitle", "click", view.showDocument);
	
	view.documentModel = new view.DocumentViewModel();
	//draw circles of documents
	ko.bindingHandlers.drawCircles = {
	    update: function(elem, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {		
			var child = $(elem).children('svg');
			$.each(valueAccessor(), function(index, value){
				var topic = topicModel.topicList()[value];
				var circle = $(SVG('circle')).attr('r', 5).attr('cx', 10).attr('cy', 10+index*15).attr('fill', topic.topicColor).attr('title', topic.topicTitle).attr('stroke','black').attr('stroke-width', '0.5').attr('class','t_'+topic.topicId);
				child.append(circle);
			});
	    }
	};
	
	//assign documentModel to GUI
	ko.applyBindings(view.documentModel, this.content[0]);
	
	if(documentTitleFields.length > 1 || documentBodyFields.length > 1) {
		var template;
		$('#test').before('<div><img class="menuActivator rotate1" src="images/Black_Settings.png" alt="&gt;"/></div>' 
			+ '<div id="browserMenu" style="display: none;background-color: #EEEEEE; border: 1px solid #D1D1D1; position: absolute; z-index: 1;"/>');
		if(documentTitleFields.length > 1) {
			template = 'DocTitle: <ul id="documentTitleMenu" class="menu" > ';
			$.each(documentTitleFields, function(key, value) {
				template += '<li><a href="#">' + value + '</a></li>';
			});
			template += '</ul>';
			$('#browserMenu').html(template);
		}
		if(documentBodyFields.length > 1) {
			template = 'DocBody: <ul id="documentBodyMenu" class="menu"> ';
			$.each(documentBodyFields, function(key, value) {
				template += '<li><a href="#">' + value + '</a></li>';
			});
			template += '</ul>';
			$('#browserMenu').html($('#browserMenu').html() + template);
		}
	}
};

ko.observableArray.fn.sortByPropertyAsc = function(prop) {
    this.sort(function(obj1, obj2) {
//		console.log(obj1["DOCUMENT_ID"] + ": " + obj1[prop] + ", " + obj2["DOCUMENT_ID"] + ": " + obj2[prop]);
        if (obj1[prop] == obj2[prop])
            return 0;
        else if (obj1[prop] < obj2[prop])
            return -1;
        else
            return 1;
    });
};

view.getDocumentViewModel = function (sorting, dir) {
	var view = this;
	var self = new Object();
	self.documentData = ko.observableArray();
	$.each(topicExplorer.jsonModel.DOCUMENT, function( key, document ) {
		if(key != '__ko_mapping__')
			self.documentData()[key] = new view.DocumentModel(document);
	});
	
	if(sorting !==  'undefined') {
		if(dir === 'undefined')  {
			dir = 1;
		}
	
		if(dir == 1) {
			console.log("ASC");
			self.documentData.sort(function(obj1, obj2) {
		        if (obj1[sorting] == obj2[sorting])
		            return 0;
		        else if (obj1[sorting] < obj2[sorting])
		            return -1;
		        else
		            return 1;
		    });
		} else {
			console.log("DESC");
			self.documentData.sort(function(obj1, obj2) {
		        if (obj1[sorting] == obj2[sorting])
		            return 0;
		        else if (obj1[sorting] < obj2[sorting])
		            return 1;
		        else
		            return -1;
		    });
		}
	}	

	return self;
};

view.DocumentViewModel = function () {
	var self = this;
	self.documentData = ko.observableArray();
	$.each(topicExplorer.jsonModel.DOCUMENT, function( key, document ) {
		
		if(key != '__ko_mapping__')
			self.documentData()[key] = new view.DocumentModel(document);
	});
//	console.log(self.documentData());
};

view.DocumentModel = function (document, docTitleIdx, docBodyIdx) {
//	console.log(document);
//	self.documentTitle = ko.observableArray();
	if(!docTitleIdx)docTitleIdx = 0;
	if(!docBodyIdx)docBodyIdx = 0;
	var self = this;
	self.id = document.DOCUMENT_ID();
	self.relevanzen = ko.observableArray(document.TOP_TOPIC());
	self.topTopics = self.relevanzen.slice(0,4);
	if(typeof document.PR_DOCUMENT_GIVEN_TOPIC == "function") {
		self.topicRelevance = document.PR_DOCUMENT_GIVEN_TOPIC(); 
	}
	
	if(documentTitleFields.length > 0) {
		self.documentTitle = ko.observable(document[documentTitleFields[docTitleIdx]]());
		$.each(documentTitleFields, function(key, value){
			self[value] = document[value]();
		});
	} else {
		self.documentTitle = ko.observable("Document " + document.DOCUMENT_ID());
	}
	if(documentBodyFields.length > 0) {
		self.documentBody = document[documentBodyFields[docBodyIdx]]();
		$.each(documentBodyFields, function(key, value){
			self[value] = document[value]();
		});
	}else {
		self.documentBody = "";
	}
};

view.showDocument = function (e) {
	var doc = $(e.currentTarget).parents('li').attr('id').split('doc_')[1];
	var wordList = null;
	
	
	$.getJSON('JsonServlet', {Command:'getDoc', DocId:doc})
	.done(function(json) {
		doc = json.DOCUMENT;	
		wordList = json.WORD_LIST;
	});
	
	if(documentTitleFields.length > 0) {
		title = doc[documentTitleFields[0]];
	} else {
		title = "Document " + doc.DOCUMENT_ID;
	}
	if(documentBodyFields.length > 0) {
		text = doc[documentBodyFields[0]];
		for(var i = 0; i < wordList.length; i++) {
			var topic = topicModel.topicList()[wordList[i].TOPIC_ID];
			text = text.substring(0, wordList[i].POSITION_OF_TOKEN_IN_DOCUMENT) + '<span style="background-color: '+topic.topicColor+'">' + wordList[i].TOKEN + "</span>" + text.substring(Number(wordList[i].POSITION_OF_TOKEN_IN_DOCUMENT) + wordList[i].TOKEN.length);
		}
	}else {
		text = "";
	}
	
	
	gui.drawTab(title,true,true, "<h2>" + title + "</h2>" + text);
	e.preventDefault();
	return false;
};

view.setDocumentModel = function () {
	//documentModel = new view.DocumentViewModel();
};

view.Template = function () {
	var template = '<ul class="browser" data-bind="foreach: $root.documentData">'+
	'<!-- ko if: hasOwnProperty("id") -->'+
	'<li class="documents" data-bind="attr: { \'id\': \'doc_\'+$data.id }">'+
		'<div class="docButtons">'+
			'<button class="addButton" type="button" title="add to shortlist" name="addToCart"></button>'+
			'<button class="chartButton" type="button" title="show topic mixture" name="showBig" onclick=""></button>'+
		'</div>'+
		'<a href="#" class="docTitle"><nobr data-bind="text: documentTitle"></nobr></a>'+
		'<p class="docContent"><span data-bind="text: documentBody"></span></p>'+
		'<div class="circles" data-bind="drawCircles: topTopics">'+
			'<svg width="100%" height="100%"></svg>'+
		'</div>'+
	'</li>'+
	'<!-- /ko -->'+
	'</ul>';
	return template;
};
view.getTemplate = function ()
{
	var template = $('<div>').attr('class', 'documentList').html(this.Template());
	return template;
};