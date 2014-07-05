define(
    [ "knockout", "jquery", "moment", "highstock", "filesaver"],
    function(ko, $, moment) {
    	self.leftBodyHeight=ko.observable(topicexplorerModel.view.leftBodyHeight).subscribeTo("leftBodyHeight");
    	
    	
    	
    	self.timeDesktopHeight= ko.computed(function() {
    		return ((self.leftBodyHeight() - 90) * 0.7);
    	});
    	
    	self.timeScrollCallback = function(el) {
			$("#singleMenuActivator, #singleMenu").css('top', $("#desktop").scrollTop());
		};
		
		self.timeDesktopHeight.subscribe(function() {
			self.makeChart();
		});
		
		
		self.singlePluginTemplates = topicexplorerModel.config.singleView.pluginTemplates;
		self.singlePluginTemplate = ko.observable(self.singlePluginTemplates[topicexplorerModel.config.singleView.activePlugin]);
		self.singlePluginTemplate.subscribe(function(newValue) {
			topicexplorerModel.config.singleView.activePlugin = topicexplorerModel.config.singleView.pluginTemplates.indexOf(newValue);
		});
		
		self.activeTab = ko.observable(topicexplorerModel.view.activeTab)
			.subscribeTo("TabView.activeTab");
		
		self.activeTab.subscribe(function(newValue) {
			if(topicexplorerModel.view.tab[newValue].module == "time-view") {
				self.topicId(topicexplorerModel.view.tab[newValue].focus);
				if(typeof topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics == 'undefined')
	    			topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics = new Array("" + self.topicId());
	    		self.renderedTopics(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics);
	    		if($("#chart").size())
				self.makeChart();
			}
		});
		
		
    	self.topicId = ko.observable(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].focus);
    	
    	self.renderedTopics = ko.observableArray();
		self.renderedTopics.subscribe(function(newValue) {
			if(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics.length > newValue.length) {
				var diff = $(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics).not(newValue).get();
				self.chart.get('topicChart' + diff[0]).remove();
			} else if(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics.length < newValue.length) {
				var diff = $(newValue).not(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics).get();
				self.addChart(diff[0]);
			}
			topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics = newValue.slice();
		});
    	
    	self.makeChart = function() {
    		self.topicId(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].focus);
    		self.chart = new Highcharts.StockChart({
		   	    chart: {
		    	   renderTo: 'chart',
		    	   height: self.timeDesktopHeight() - ($('#topicCheckboxes').height() + 30.0 * $('#getcsv').size())
		    	},
		    	tooltip: {
		            formatter: function() {
		            	var html ="";
		            	if(typeof topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics != 'undefined') {
			            	for(var i = 0; i < topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics.length; i++ ) {
				            	html += '<span style="font-size: x-large; font-family: Helvetica, Arial, sans-serif;text-shadow: -0.5px 0 black, 0 0.5px black, 0.5px 0 black, 0 -0.5px black;color: ' 
				            		+ topicexplorerModel.data.topic[topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics[i]].COLOR_TOPIC$COLOR	+ '">● </span><span>'; 
				            	if(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics[i] != 'average')
				            		html += topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics[i] + ": "; 
				            	html +=  topicexplorerModel.data.topic[topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics[i]].TIME$WORDS_PER_WEEK[this.x].LABEL 
				            		+ '</span>: ' + topicexplorerModel.data.topic[topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics[i]].TIME$WORDS_PER_WEEK[this.x].WORD_COUNT
				            		+ '<br/>';
		            		}
		            	}
		            	return html;
		            }
		    	}
		    });
    		if(typeof topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics != 'undefined') {
	    		for(var i = 0; i < topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics.length; i++ ) {
	    			self.addChart(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics[i]);
	    		}
    		}
    	};
    	
    	self.addChart = function(topicId) {
    		self.chart.addSeries({
    			id: 'topicChart' + topicId,
    			name: 'topicChart' + topicId,
    			data: (function() {
    				var data = new Array();
    				for(key in topicexplorerModel.data.topic[topicId].TIME$WORDS_PER_WEEK) {
    					data.push(new Array(parseInt(key), topicexplorerModel.data.topic[topicId].TIME$WORDS_PER_WEEK[key].WORD_COUNT));
    				}
    				return data;
    			})(),
		    	color: topicexplorerModel.data.topic[topicId].COLOR_TOPIC$COLOR,
		    	events: {
		    		click: function(event) {
		    			self.loadDocumentsForTopicsAndWeek(event.point.x);
		    		}
		    	}
    		});
    	};
    	
    	self.init = function() {
    		if(typeof topicexplorerModel.data.timeDataLoaded == 'undefined') {
    			topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics = new Array("" + self.topicId());
	    		self.renderedTopics(topicexplorerModel.view.tab[topicexplorerModel.view.activeTab].renderedTopics);
				$.getJSON("JsonServlet?Command=getDates").success(function(receivedParsedJson) {
					var avg = new Object();
					
					for (key in receivedParsedJson) {
						$.extend(self.topicexplorerModel.data.topic[key], receivedParsedJson[key]);
					    $.each(receivedParsedJson[key].TIME$WORDS_PER_WEEK, function(index, value) {
				    		if(avg[index] == null) avg[index] = 0;
				    		avg[index] += value.WORD_COUNT;

					    });
					}
					var topicCount = Object.keys(topicexplorerModel.data.topic).length;
					
					topicexplorerModel.data.topic.average = new Object();
					topicexplorerModel.data.topic.average.COLOR_TOPIC$COLOR = "#000000";
					topicexplorerModel.data.topic.average.TIME$WORDS_PER_WEEK = new Object();
					$.each(avg , function(weekStamp, value) {
						topicexplorerModel.data.topic.average.TIME$WORDS_PER_WEEK[weekStamp] = {'WORD_COUNT':value / topicCount, 'LABEL': 'Average'};
					});
					topicexplorerModel.data.timeDataLoaded = 1;
					self.makeChart();
				});
			} else {
				self.makeChart();
			}
    	};
    	
    	self.loadDocumentsForTopicsAndWeek = function (week) { 
    		date = new Date(week);
//    		topics = new Array();
//    		for(topic in self.renderedTopics()) {
//    			if(self.renderedTopics()[topic] != "average") 
//    				topics.push(self.renderedTopics()[topic]);
//    		}
    		topicexplorerModel.newTab("Command=bestDocs&TopicId="+ self.renderedTopics()[0] + "&week=" + week.toString().substr(0,10), date.getDate() + "." + (date.getMonth() + 1) + "." + date.getFullYear().toString().substr(2,2) + " (" + self.renderedTopics()[0] + ")", 'document-view', [{"topic": self.topicId()}]);	
    	};
    	
    	self.downloadCSV = function() {
    		try {
    		    var isFileSaverSupported = !!new Blob;
    		    var out = "id;color";
        		for(tstamp in topicexplorerModel.data.topic[0].TIME$WORDS_PER_WEEK)
        			out += ";" + tstamp;
        		out += "\n";
        		for(topic in self.renderedTopics()) {
        			out += self.renderedTopics()[topic] + ";" + topicexplorerModel.data.topic[self.renderedTopics()[topic]].COLOR_TOPIC$COLOR;
        			if(self.renderedTopics()[topic] != "average") {
        				for(tstamp in topicexplorerModel.data.topic[self.renderedTopics()[topic]].TIME$WORDS_PER_WEEK) {
        					out += ";" + topicexplorerModel.data.topic[self.renderedTopics()[topic]].TIME$WORDS_PER_WEEK[tstamp].WORD_COUNT;
        				}
        				out += "\n";
        			} 		
        		}
        		var blob = new Blob([out], {type: "application/csv;charset=utf-8"});
        		saveAs(blob, "data.csv");
    		} catch (e) {
    			alert("Download is not supported by your browser - please update!");
    		}
    	};
    	
    	return self;
    }); 