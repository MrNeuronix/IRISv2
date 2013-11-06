window.org_agocontrol_site_component_flot_Flot = function() {

    function mergeOptions(obj1,obj2){
        var obj3 = {};
        for (var attrname in obj1) { obj3[attrname] = obj1[attrname]; }
        for (var attrname in obj2) { obj3[attrname] = obj2[attrname]; }
        return obj3;
    }

    this.onStateChange = function() {
          var container = this.getElement()
          var
            d1    = [],
            start = new Date("2009/01/01 01:00").getTime(),
            options,
            graph,
            i, x, o;

          for (i = 0; i < 100; i++) {
            x = start+(i*1000*3600*24*36.5);
            d1.push([x, i+Math.random()*30+Math.sin(i/20+Math.random()*2)*20+Math.sin(i/10+Math.random())*10]);
          }



          var dataSets = this.getState().dataSets;

          var nestedOptions =  this.getState().options;
          options = nestedOptions.options;
          options = mergeOptions(options, nestedOptions);


          // Draw graph with default options, overwriting with passed options
          function drawGraph (opts) {

            // Clone the options, so the 'options' variable always keeps intact.
            o = Flotr._.extend(Flotr._.clone(options), opts || {});

            // Return a new graph.
            return Flotr.draw(
              container,
              dataSets,
              o
            );
          }

          graph = drawGraph();

          Flotr.EventAdapter.observe(container, 'flotr:select', function(area){
            // Draw selected area
            var opts = Flotr._.extend(Flotr._.clone(options), {});
            opts.xaxis.min = area.x1;
            opts.xaxis.max = area.x2;
            opts.yaxis.min = area.y1;
            opts.yaxis.max = area.y2;
            graph = drawGraph(opts);
          });

          // When graph is clicked, draw the graph with default area.
          Flotr.EventAdapter.observe(container, 'flotr:click', function () { graph = drawGraph(); });

    }
}
