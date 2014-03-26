define(
    [ "knockout", "jquery", "scripts/modules/tab-view"],
    function(ko, $) {
    	self.leftBodyHeight=ko.observable(topicexplorerModel.view.leftBodyHeight).subscribeTo("leftBodyHeight");
    	
    	self.desktopHeight= ko.computed(function() {
    		return ((self.leftBodyHeight() - 90) * 0.7);
    	});
   		
   		self.singlePluginTemplates = topicexplorerModel.config.singleView.pluginTemplates;
		self.singlePluginTemplate = ko.observable(self.singlePluginTemplates[topicexplorerModel.config.singleView.activePlugin]);
		self.singlePluginTemplate.subscribe(function(newValue) {
			topicexplorerModel.config.singleView.activePlugin = topicexplorerModel.config.singleView.pluginTemplates.indexOf(newValue);
		});

    	self.selectedDocuments = ko.observable(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].documentSorting).subscribeTo('DocumentView.selectedDocuments');
    	
    	self.markWords = function() {
    		if(!topicexplorerModel.data.document[self.selectedDocuments()[0]].TEXT_WITH_MARKED_WORDS) {
	    		var text = topicexplorerModel.data.document[self.selectedDocuments()[0]].TEXT$FULLTEXT;
	    		var words = topicexplorerModel.data.document[self.selectedDocuments()[0]].WORD_LIST;
	    		for(key in words) {
	    			text = text.substring(0, words[key].POSITION_OF_TOKEN_IN_DOCUMENT) 
	    				+ '<span style="border-bottom: 2px solid ' + topicexplorerModel.data.topic[words[key].TOPIC_ID].COLOR_TOPIC$COLOR + ';">' 
	    				+ words[key].TOKEN + '</span>' + text.substring(parseInt(words[key].POSITION_OF_TOKEN_IN_DOCUMENT) + words[key].TOKEN.length);
	    		}
	    		topicexplorerModel.data.document[self.selectedDocuments()[0]].TEXT_WITH_MARKED_WORDS = text;
    		}
    		return topicexplorerModel.data.document[self.selectedDocuments()[0]].TEXT_WITH_MARKED_WORDS;
    	};
    	
    	self.singleScrollCallback = function(el) {
			$("#singleMenuActivator, #singleMenu").css('top', $("#desktop").scrollTop());
		};
		
    	return self;
    }); 
