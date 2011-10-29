// Copyright (c) 2005 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
// Contributors:
//  Justin Palmer (http://encytemedia.com/)
//  Mark Pilgrim (http://diveintomark.org/)
//  Martin Bialasinki
// 
// See scriptaculous.js for full license.

var Effect = {
  tagifyText: function(element) {
    var tagifyStyle = "position:relative";
    if(/MSIE/.test(navigator.userAgent)) tagifyStyle += ";zoom:1";
    element = $(element);
    $A(element.childNodes).each( function(child) {
      if(child.nodeType==3) {
        child.nodeValue.toArray().each( function(character) {
          element.insertBefore(
            Builder.node('span',{style: tagifyStyle},
              character == " " ? String.fromCharCode(160) : character), 
              child);
        });
        Element.remove(child);
      }
    });
  },
  multiple: function(element, effect) {
    var elements;
    if(((typeof element == 'object') || 
        (typeof element == 'function')) && 
       (element.length))
      elements = element;
    else
      elements = $(element).childNodes;
      
    var options = Object.extend({
      speed: 0.1,
      delay: 0.0
    }, arguments[2] || {});
    var speed = options.speed;
    var delay = options.delay;

    $A(elements).each( function(element, index) {
      new effect(element, Object.extend(options, { delay: delay + index * speed }));
    });
  }
};

var Effect2 = Effect; // deprecated

/* ------------- transitions ------------- */

Effect.Transitions = {}

Effect.Transitions.linear = function(pos) {
  return pos;
}
Effect.Transitions.sinoidal = function(pos) {
  return (-Math.cos(pos*Math.PI)/2) + 0.5;
}
Effect.Transitions.reverse  = function(pos) {
  return 1-pos;
}
Effect.Transitions.flicker = function(pos) {
  return ((-Math.cos(pos*Math.PI)/4) + 0.75) + Math.random()/4;
}
Effect.Transitions.wobble = function(pos) {
  return (-Math.cos(pos*Math.PI*(9*pos))/2) + 0.5;
}
Effect.Transitions.pulse = function(pos) {
  return (Math.floor(pos*10) % 2 == 0 ? 
    (pos*10-Math.floor(pos*10)) : 1-(pos*10-Math.floor(pos*10)));
}
Effect.Transitions.none = function(pos) {
  return 0;
}
Effect.Transitions.full = function(pos) {
  return 1;
}

/* ------------- core effects ------------- */

Effect.Queue = {
  effects:  [],
  interval: null,
  add: function(effect) {
    var timestamp = new Date().getTime();
    
    switch(effect.options.queue) {
      case 'front':
        // move unstarted effects after this effect  
        this.effects.findAll(function(e){ return e.state=='idle' }).each( function(e) {
            e.startOn  += effect.finishOn;
            e.finishOn += effect.finishOn;
          });
        break;
      case 'end':
        // start effect after last queued effect has finished
        timestamp = this.effects.pluck('finishOn').max() || timestamp;
        break;
    }
    
    effect.startOn  += timestamp;
    effect.finishOn += timestamp;
    this.effects.push(effect);
    if(!this.interval) 
      this.interval = setInterval(this.loop.bind(this), 40);
  },
  remove: function(effect) {
    this.effects = this.effects.reject(function(e) { return e==effect });
    if(this.effects.length == 0) {
      clearInterval(this.interval);
      this.interval = null;
    }
  },
  loop: function() {
    var timePos = new Date().getTime();
    this.effects.invoke('loop', timePos);
  }
}

Effect.Base = function() {};
Effect.Base.prototype = {
  position: null,
  setOptions: function(options) {
    this.options = Object.extend({
      transition: Effect.Transitions.sinoidal,
      duration:   1.0,   // seconds
      fps:        25.0,  // max. 25fps due to Effect.Queue implementation
      sync:       false, // true for combining
      from:       0.0,
      to:         1.0,
      delay:      0.0,
      queue:      'parallel'
    }, options || {});
  },
  start: function(options) {
    this.setOptions(options || {});
    this.currentFrame = 0;
    this.state        = 'idle';
    this.startOn      = this.options.delay*1000;
    this.finishOn     = this.startOn + (this.options.duration*1000);
    this.event('beforeStart');
    if(!this.options.sync) Effect.Queue.add(this);
  },
  loop: function(timePos) {
    if(timePos >= this.startOn) {
      if(timePos >= this.finishOn) {
        this.render(1.0);
        this.cancel();
        this.event('beforeFinish');
        if(this.finish) this.finish(); 
        this.event('afterFinish');
        return;  
      }
      var pos   = (timePos - this.startOn) / (this.finishOn - this.startOn);
      var frame = Math.round(pos * this.options.fps * this.options.duration);
      if(frame > this.currentFrame) {
        this.render(pos);
        this.currentFrame = frame;
      }
    }
  },
  render: function(pos) {
    if(this.state == 'idle') {
      this.state = 'running';
      this.event('beforeSetup');
      if(this.setup) this.setup();
      this.event('afterSetup');
    }
    if(this.options.transition) pos = this.options.transition(pos);
    pos *= (this.options.to-this.options.from);
    pos += this.options.from;
    this.position = pos;
    this.event('beforeUpdate');
    if(this.update) this.update(pos);
    this.event('afterUpdate');
  },
  cancel: function() {
    if(!this.options.sync) Effect.Queue.remove(this);
    this.state = 'finished';
  },
  event: function(eventName) {
    if(this.options[eventName + 'Internal']) this.options[eventName + 'Internal'](this);
    if(this.options[eventName]) this.options[eventName](this);
  }
}

Effect.Parallel = Class.create();
Object.extend(Object.extend(Effect.Parallel.prototype, Effect.Base.prototype), {
  initialize: function(effects) {
    this.effects = effects || [];
    this.start(arguments[1]);
  },
  update: function(position) {
    this.effects.invoke('render', position);
  },
  finish: function(position) {
    this.effects.each( function(effect) {
      effect.render(1.0);
      effect.cancel();
      effect.event('beforeFinish');
      if(effect.finish) effect.finish(position);
      effect.event('afterFinish');
    });
  }
});

Effect.Opacity = Class.create();
Object.extend(Object.extend(Effect.Opacity.prototype, Effect.Base.prototype), {
  initialize: function(element) {
    this.element = $(element);
    // make this work on IE on elements without 'layout'
    if(/MSIE/.test(navigator.userAgent) && (!this.element.hasLayout))
      this.element.style.zoom = 1;
    var options = Object.extend({
      from: Element.getOpacity(this.element) || 0.0,
      to:   1.0
    }, arguments[1] || {});
    this.start(options);
  },
  update: function(position) {
    Element.setOpacity(this.element, position);
  }
});

Effect.MoveBy = Class.create();
Object.extend(Object.extend(Effect.MoveBy.prototype, Effect.Base.prototype), {
  initialize: function(element, toTop, toLeft) {
    this.element      = $(element);
    this.toTop        = toTop;
    this.toLeft       = toLeft;
    this.start(arguments[3]);
  },
  setup: function() {
    // Bug in Opera: Opera returns the "real" position of a static element or
    // relative element that does not have top/left explicitly set.
    // ==> Always set top and left for position relative elements in your stylesheets 
    // (to 0 if you do not need them)
    
    Element.makePositioned(this.element);
    this.originalTop  = parseFloat(Element.getStyle(this.element,'top')  || '0');
    this.originalLeft = parseFloat(Element.getStyle(this.element,'left') || '0');
  },
  update: function(position) {
    var topd  = this.toTop  * position + this.originalTop;
    var leftd = this.toLeft * position + this.originalLeft;
    this.setPosition(topd, leftd);
  },
  setPosition: function(topd, leftd) {
    this.element.style.top  = topd  + "px";
    this.element.style.left = leftd + "px";
  }
});

Effect.Scale = Class.create();
Object.extend(Object.extend(Effect.Scale.prototype, Effect.Base.prototype), {
  initialize: function(element, percent) {
    this.element = $(element)
    var options = Object.extend({
      scaleX: true,
      scaleY: true,
      scaleContent: true,
      scaleFromCenter: false,
      scaleMode: 'box',        // 'box' or 'contents' or {} with provided values
      scaleFrom: 100.0,
      scaleTo:   percent
    }, arguments[2] || {});
    this.start(options);
  },
  setup: function() {
    var effect = this;
    
    this.restoreAfterFinish = this.options.restoreAfterFinish || false;
    this.elementPositioning = Element.getStyle(this.element,'position');
    
    effect.originalStyle = {};
    ['top','left','width','height','fontSize'].each( function(k) {
      effect.originalStyle[k] = effect.element.style[k];
    });
      
    this.originalTop  = this.element.offsetTop;
    this.originalLeft = this.element.offsetLeft;
    
    var fontSize = Element.getStyle(this.element,'font-size') || "100%";
    ['em','px','%'].each( function(fontSizeType) {
      if(fontSize.indexOf(fontSizeType)>0) {
        effect.fontSize     = parseFloat(fontSize);
        effect.fontSizeType = fontSizeType;
      }
    });
    
    this.factor = (this.options.scaleTo - this.options.scaleFrom)/100;
    
    this.dims = null;
    if(this.options.scaleMode=='box')
      this.dims = [this.element.clientHeight, this.element.clientWidth];
    if(this.options.scaleMode=='content')
      this.dims = [this.element.scrollHeight, this.element.scrollWidth];
    if(!this.dims)
      this.dims = [this.options.scaleMode.originalHeight,
                   this.options.scaleMode.originalWidth];
  },
  update: function(position) {
    var currentScale = (this.options.scaleFrom/100.0) + (this.factor * position);
    if(this.options.scaleContent && this.fontSize)
      this.element.style.fontSize = this.fontSize*currentScale + this.fontSizeType;
    this.setDimensions(this.dims[0] * currentScale, this.dims[1] * currentScale);
  },
  finish: function(position) {
    if (this.restoreAfterFinish) {
      var effect = this;
      ['top','left','width','height','fontSize'].each( function(k) {
        effect.element.style[k] = effect.originalStyle[k];
      });
    }
  },
  setDimensions: function(height, width) {
    var els = this.element.style;
    if(this.options.scaleX) els.width = width + 'px';
    if(this.options.scaleY) els.height = height + 'px';
    if(this.options.scaleFromCenter) {
      var topd  = (height - this.dims[0])/2;
      var leftd = (width  - this.dims[1])/2;
      if(this.elementPositioning == 'absolute') {
        if(this.options.scaleY) els.top = this.originalTop-topd + "px";
        if(this.options.scaleX) els.left = this.originalLeft-leftd + "px";
      } else {
        if(this.options.scaleY) els.top = -topd + "px";
        if(this.options.scaleX) els.left = -leftd + "px";
      }
    }
  }
});

Effect.Highlight = Class.create();
Object.extend(Object.extend(Effect.Highlight.prototype, Effect.Base.prototype), {
  initialize: function(element) {
    this.element = $(element);
    var options = Object.extend({
      startcolor:   "#ffff99"
    }, arguments[1] || {});
    this.start(options);
  },
  setup: function() {
    // Disable background image during the effect
    this.oldBgImage = this.element.style.backgroundImage;
    this.element.style.backgroundImage = "none";
    if(!this.options.endcolor)
      this.options.endcolor = Element.getStyle(this.element, 'background-color').parseColor('#ffffff');
    if (typeof this.options.restorecolor == "undefined")
      this.options.restorecolor = this.element.style.backgroundColor;
    // init color calculations
    this.colors_base = [
      parseInt(this.options.startcolor.slice(1,3),16),
      parseInt(this.options.startcolor.slice(3,5),16),
      parseInt(this.options.startcolor.slice(5),16) ];
    this.colors_delta = [
      parseInt(this.options.endcolor.slice(1,3),16)-this.colors_base[0],
      parseInt(this.options.endcolor.slice(3,5),16)-this.colors_base[1],
      parseInt(this.options.endcolor.slice(5),16)-this.colors_base[2]];
  },
  update: function(position) {
    var effect = this; var colors = $R(0,2).map( function(i){ 
      return Math.round(effect.colors_base[i]+(effect.colors_delta[i]*position))
    });
    this.element.style.backgroundColor = "#" +
      colors[0].toColorPart() + colors[1].toColorPart() + colors[2].toColorPart();
  },
  finish: function() {
    this.element.style.backgroundColor = this.options.restorecolor;
    this.element.style.backgroundImage = this.oldBgImage;
  }
});

Effect.ScrollTo = Class.create();
Object.extend(Object.extend(Effect.ScrollTo.prototype, Effect.Base.prototype), {
  initialize: function(element) {
    this.element = $(element);
    this.start(arguments[1] || {});
  },
  setup: function() {
    Position.prepare();
    var offsets = Position.cumulativeOffset(this.element);
    var max = window.innerHeight ? 
      window.height - window.innerHeight :
      document.body.scrollHeight - 
        (document.documentElement.clientHeight ? 
          document.documentElement.clientHeight : document.body.clientHeight);
    this.scrollStart = Position.deltaY;
    this.delta = (offsets[1] > max ? max : offsets[1]) - this.scrollStart;
  },
  update: function(position) {
    Position.prepare();
    window.scrollTo(Position.deltaX, 
      this.scrollStart + (position*this.delta));
  }
});

/* ------------- combination effects ------------- */

Effect.Fade = function(element) {
  var oldOpacity = Element.getInlineOpacity(element);
  var options = Object.extend({
  from: Element.getOpacity(element) || 1.0,
  to:   0.0,
  afterFinishInternal: function(effect) 
    { if (effect.options.to == 0) {
        Element.hide(effect.element);
        Element.setInlineOpacity(effect.element, oldOpacity);
      }  
    }
  }, arguments[1] || {});
  return new Effect.Opacity(element,options);
}

Effect.Appear = function(element) {
  var options = Object.extend({
  from: (Element.getStyle(element, "display") == "none" ? 0.0 : Element.getOpacity(element) || 0.0),
  to:   1.0,
  beforeSetup: function(effect)  
    { Element.setOpacity(effect.element, effect.options.from);
      Element.show(effect.element); }
  }, arguments[1] || {});
  return new Effect.Opacity(element,options);
}

Effect.Puff = function(element) {
  element = $(element);
  var oldOpacity = Element.getInlineOpacity(element);
  var oldPosition = element.style.position;
  return new Effect.Parallel(
   [ new Effect.Scale(element, 200, 
      { sync: true, scaleFromCenter: true, scaleContent: true, restoreAfterFinish: true }), 
     new Effect.Opacity(element, { sync: true, to: 0.0 } ) ], 
     Object.extend({ duration: 1.0, 
      beforeSetupInternal: function(effect) 
       { effect.effects[0].element.style.position = 'absolute'; },
      afterFinishInternal: function(effect)
       { Element.hide(effect.effects[0].element);
         effect.effects[0].element.style.position = oldPosition;
         Element.setInlineOpacity(effect.effects[0].element, oldOpacity); }
     }, arguments[1] || {})
   );
}

Effect.BlindUp = function(element) {
  element = $(element);
  Element.makeClipping(element);
  return new Effect.Scale(element, 0, 
    Object.extend({ scaleContent: false, 
      scaleX: false, 
      restoreAfterFinish: true,
      afterFinishInternal: function(effect)
        { 
          Element.hide(effect.element);
          Element.undoClipping(effect.element);
        } 
    }, arguments[1] || {})
  );
}

Effect.BlindDown = function(element) {
  element = $(element);
  var oldHeight = element.style.height;
  var elementDimensions = Element.getDimensions(element);
  return new Effect.Scale(element, 100, 
    Object.extend({ scaleContent: false, 
      scaleX: false,
      scaleFrom: 0,
      scaleMode: {originalHeight: elementDimensions.height, originalWidth: elementDimensions.width},
      restoreAfterFinish: true,
      afterSetup: function(effect) {
        Element.makeClipping(effect.element);
        effect.element.style.height = "0px";
        Element.show(effect.element); 
      },  
      afterFinishInternal: function(effect) {
        Element.undoClipping(effect.element);
        effect.element.style.height = oldHeight;
      }
    }, arguments[1] || {})
  );
}

Effect.SwitchOff = function(element) {
  element = $(element);
  var oldOpacity = Element.getInlineOpacity(element);
  return new Effect.Appear(element, { 
    duration: 0.4,
    from: 0,
    transition: Effect.Transitions.flicker,
    afterFinishInternal: function(effect) {
      new Effect.Scale(effect.element, 1, { 
        duration: 0.3, scaleFromCenter: true,
        scaleX: false, scaleContent: false, restoreAfterFinish: true,
        beforeSetup: function(effect) { 
          Element.makePositioned(effect.element); 
          Element.makeClipping(effect.element);
        },
        afterFinishInternal: function(effect) { 
          Element.hide(effect.element); 
          Element.undoClipping(effect.element);
          Element.undoPositioned(effect.element);
          Element.setInlineOpacity(effect.element, oldOpacity);
        }
      })
    }
  });
}

Effect.DropOut = function(element) {
  element = $(element);
  var oldTop = element.style.top;
  var oldLeft = element.style.left;
  var oldOpacity = Element.getInlineOpacity(element);
  return new Effect.Parallel(
    [ new Effect.MoveBy(element, 100, 0, { sync: true }), 
      new Effect.Opacity(element, { sync: true, to: 0.0 }) ],
    Object.extend(
      { duration: 0.5,
        beforeSetup: function(effect) { 
          Element.makePositioned(effect.effects[0].element); },
        afterFinishInternal: function(effect) { 
          Element.hide(effect.effects[0].element); 
          Element.undoPositioned(effect.effects[0].element);
          effect.effects[0].element.style.left = oldLeft;
          effect.effects[0].element.style.top = oldTop;
          Element.setInlineOpacity(effect.effects[0].element, oldOpacity); } 
      }, arguments[1] || {}));
}

Effect.Shake = function(element) {
  element = $(element);
  var oldTop = element.style.top;
  var oldLeft = element.style.left;
  return new Effect.MoveBy(element, 0, 20, 
    { duration: 0.05, afterFinishInternal: function(effect) {
  new Effect.MoveBy(effect.element, 0, -40, 
    { duration: 0.1, afterFinishInternal: function(effect) {
  new Effect.MoveBy(effect.element, 0, 40, 
    { duration: 0.1, afterFinishInternal: function(effect) {
  new Effect.MoveBy(effect.element, 0, -40, 
    { duration: 0.1, afterFinishInternal: function(effect) {
  new Effect.MoveBy(effect.element, 0, 40, 
    { duration: 0.1, afterFinishInternal: function(effect) {
  new Effect.MoveBy(effect.element, 0, -20, 
    { duration: 0.05, afterFinishInternal: function(effect) {
        Element.undoPositioned(effect.element);
        effect.element.style.left = oldLeft;
        effect.element.style.top = oldTop;
  }}) }}) }}) }}) }}) }});
}

Effect.SlideDown = function(element) {
  element = $(element);
  Element.cleanWhitespace(element);
  // SlideDown need to have the content of the element wrapped in a container element with fixed height!
  var oldInnerBottom = element.firstChild.style.bottom;
  var elementDimensions = Element.getDimensions(element);
  return new Effect.Scale(element, 100, 
   Object.extend({ scaleContent: false, 
    scaleX: false, 
    scaleFrom: 0,
    scaleMode: {originalHeight: elementDimensions.height, originalWidth: elementDimensions.width},    
    restoreAfterFinish: true,
    afterSetup: function(effect) {
      Element.makePositioned(effect.element.firstChild);
      if (window.opera) effect.element.firstChild.style.top = "";
      Element.makeClipping(effect.element);
      element.style.height = '0';
      Element.show(element); 
    },  
    afterUpdateInternal: function(effect) { 
      effect.element.firstChild.style.bottom = 
        (effect.originalHeight - effect.element.clientHeight) + 'px'; },
    afterFinishInternal: function(effect) { 
      Element.undoClipping(effect.element); 
      Element.undoPositioned(effect.element.firstChild);
      effect.element.firstChild.style.bottom = oldInnerBottom; }
    }, arguments[1] || {})
  );
}
  
Effect.SlideUp = function(element) {
  element = $(element);
  Element.cleanWhitespace(element);
  var oldInnerBottom = element.firstChild.style.bottom;
  return new Effect.Scale(element, 0, 
   Object.extend({ scaleContent: false, 
    scaleX: false, 
    scaleMode: 'box',
    scaleFrom: 100,
    restoreAfterFinish: true,
    beforeStartInternal: function(effect) { 
      Element.makePositioned(effect.element.firstChild);
      if (window.opera) effect.element.firstChild.style.top = "";
      Element.makeClipping(effect.element);
      Element.show(element); 
    },  
    afterUpdateInternal: function(effect) { 
     effect.element.firstChild.style.bottom = 
       (effect.originalHeight - effect.element.clientHeight) + 'px'; },
    afterFinishInternal: function(effect) { 
        Element.hide(effect.element);
        Element.undoClipping(effect.element); 
        Element.undoPositioned(effect.element.firstChild);
        effect.element.firstChild.style.bottom = oldInnerBottom; }
   }, arguments[1] || {})
  );
}

Effect.Squish = function(element) {
  // Bug in opera makes the TD containing this element expand for a instance after finish 
  return new Effect.Scale(element, window.opera ? 1 : 0, 
    { restoreAfterFinish: true,
      beforeSetup: function(effect) { 
        Element.makeClipping(effect.element); },  
      afterFinishInternal: function(effect) { 
        Element.hide(effect.element); 
        Element.undoClipping(effect.element); } 
  });
}

Effect.Grow = function(element) {
  element = $(element);
  var options = arguments[1] || {};
  
  var elementDimensions = Element.getDimensions(element);
  var originalWidth = elementDimensions.width;
  var originalHeight = elementDimensions.height;
  var oldTop = element.style.top;
  var oldLeft = element.style.left;
  var oldHeight = element.style.height;
  var oldWidth = element.style.width;
  var oldOpacity = Element.getInlineOpacity(element);
  
  var direction = options.direction || 'center';
  var moveTransition = options.moveTransition || Effect.Transitions.sinoidal;
  var scaleTransition = options.scaleTransition || Effect.Transitions.sinoidal;
  var opacityTransition = options.opacityTransition || Effect.Transitions.full;
  
  var initialMoveX, initialMoveY;
  var moveX, moveY;
  
  switch (direction) {
    case 'top-left':
      initialMoveX = initialMoveY = moveX = moveY = 0; 
      break;
    case 'top-right':
      initialMoveX = originalWidth;
      initialMoveY = moveY = 0;
      moveX = -originalWidth;
      break;
    case 'bottom-left':
      initialMoveX = moveX = 0;
      initialMoveY = originalHeight;
      moveY = -originalHeight;
      break;
    case 'bottom-right':
      initialMoveX = originalWidth;
      initialMoveY = originalHeight;
      moveX = -originalWidth;
      moveY = -originalHeight;
      break;
    case 'center':
      initialMoveX = originalWidth / 2;
      initialMoveY = originalHeight / 2;
      moveX = -originalWidth / 2;
      moveY = -originalHeight / 2;
      break;
  }
  
  return new Effect.MoveBy(element, initialMoveY, initialMoveX, { 
    duration: 0.01, 
    beforeSetup: function(effect) { 
      Element.hide(effect.element);
      Element.makeClipping(effect.element);
      Element.makePositioned(effect.element);
    },
    afterFinishInternal: function(effect) {
      new Effect.Parallel(
        [ new Effect.Opacity(effect.element, { sync: true, to: 1.0, from: 0.0, transition: opacityTransition }),
          new Effect.MoveBy(effect.element, moveY, moveX, { sync: true, transition: moveTransition }),
          new Effect.Scale(effect.element, 100, {
            scaleMode: { originalHeight: originalHeight, originalWidth: originalWidth }, 
            sync: true, scaleFrom: window.opera ? 1 : 0, transition: scaleTransition, restoreAfterFinish: true})
        ], Object.extend({
             beforeSetup: function(effect) {
              effect.effects[0].element.style.height = 0;
              Element.show(effect.effects[0].element);
             },              
             afterFinishInternal: function(effect) {
               var el = effect.effects[0].element;
               var els = el.style;
               Element.undoClipping(el); 
               Element.undoPositioned(el);
               els.top = oldTop;
               els.left = oldLeft;
               els.height = oldHeight;
               els.width = originalWidth;
               Element.setInlineOpacity(el, oldOpacity);
             }
           }, options)
      )
    }
  });
}

Effect.Shrink = function(element) {
  element = $(element);
  var options = arguments[1] || {};
  
  var originalWidth = element.clientWidth;
  var originalHeight = element.clientHeight;
  var oldTop = element.style.top;
  var oldLeft = element.style.left;
  var oldHeight = element.style.height;
  var oldWidth = element.style.width;
  var oldOpacity = Element.getInlineOpacity(element);

  var direction = options.direction || 'center';
  var moveTransition = options.moveTransition || Effect.Transitions.sinoidal;
  var scaleTransition = options.scaleTransition || Effect.Transitions.sinoidal;
  var opacityTransition = options.opacityTransition || Effect.Transitions.none;
  
  var moveX, moveY;
  
  switch (direction) {
    case 'top-left':
      moveX = moveY = 0;
      break;
    case 'top-right':
      moveX = originalWidth;
      moveY = 0;
      break;
    case 'bottom-left':
      moveX = 0;
      moveY = originalHeight;
      break;
    case 'bottom-right':
      moveX = originalWidth;
      moveY = originalHeight;
      break;
    case 'center':  
      moveX = originalWidth / 2;
      moveY = originalHeight / 2;
      break;
  }
  
  return new Effect.Parallel(
    [ new Effect.Opacity(element, { sync: true, to: 0.0, from: 1.0, transition: opacityTransition }),
      new Effect.Scale(element, window.opera ? 1 : 0, { sync: true, transition: scaleTransition, restoreAfterFinish: true}),
      new Effect.MoveBy(element, moveY, moveX, { sync: true, transition: moveTransition })
    ], Object.extend({            
         beforeStartInternal: function(effect) { 
           Element.makePositioned(effect.effects[0].element);
           Element.makeClipping(effect.effects[0].element);
         },
         afterFinishInternal: function(effect) {
           var el = effect.effects[0].element;
           var els = el.style;
           Element.hide(el);
           Element.undoClipping(el); 
           Element.undoPositioned(el);
           els.top = oldTop;
           els.left = oldLeft;
           els.height = oldHeight;
           els.width = oldWidth;
           Element.setInlineOpacity(el, oldOpacity);
         }
       }, options)
  );
}

Effect.Pulsate = function(element) {
  element = $(element);
  var options    = arguments[1] || {};
  var oldOpacity = Element.getInlineOpacity(element);
  var transition = options.transition || Effect.Transitions.sinoidal;
  var reverser   = function(pos){ return transition(1-Effect.Transitions.pulse(pos)) };
  reverser.bind(transition);
  return new Effect.Opacity(element, 
    Object.extend(Object.extend({  duration: 3.0, from: 0,
      afterFinishInternal: function(effect) { Element.setInlineOpacity(effect.element, oldOpacity); }
    }, options), {transition: reverser}));
}

Effect.Fold = function(element) {
  element = $(element);
  var originalTop = element.style.top;
  var originalLeft = element.style.left;
  var originalWidth = element.style.width;
  var originalHeight = element.style.height;
  Element.makeClipping(element);
  return new Effect.Scale(element, 5, Object.extend({   
    scaleContent: false,
    scaleX: false,
    afterFinishInternal: function(effect) {
    new Effect.Scale(element, 1, { 
      scaleContent: false, 
      scaleY: false,
      afterFinishInternal: function(effect) { 
        Element.hide(effect.element);  
        Element.undoClipping(effect.element); 
        effect.element.style.top = originalTop;
        effect.element.style.left = originalLeft;
        effect.element.style.width = originalWidth;
        effect.element.style.height = originalHeight;
      } });
  }}, arguments[1] || {}));
}
// Copyright (c) 2005 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
// 
// Element.Class part Copyright (c) 2005 by Rick Olson
// 
// See scriptaculous.js for full license.

/*--------------------------------------------------------------------------*/

var Droppables = {
  drops: [],

  remove: function(element) {
    this.drops = this.drops.reject(function(d) { return d.element==element });
  },

  add: function(element) {
    element = $(element);
    var options = Object.extend({
      greedy:     true,
      hoverclass: null  
    }, arguments[1] || {});

    // cache containers
    if(options.containment) {
      options._containers = [];
      var containment = options.containment;
      if((typeof containment == 'object') && 
        (containment.constructor == Array)) {
        containment.each( function(c) { options._containers.push($(c)) });
      } else {
        options._containers.push($(containment));
      }
    }

    Element.makePositioned(element); // fix IE
    options.element = element;

    this.drops.push(options);
  },

  isContained: function(element, drop) {
    var parentNode = element.parentNode;
    return drop._containers.detect(function(c) { return parentNode == c });
  },

  isAffected: function(pX, pY, element, drop) {
    return (
      (drop.element!=element) &&
      ((!drop._containers) ||
        this.isContained(element, drop)) &&
      ((!drop.accept) ||
        (Element.Class.has_any(element, drop.accept))) &&
      Position.within(drop.element, pX, pY) );
  },

  deactivate: function(drop) {
    if(drop.hoverclass)
      Element.Class.remove(drop.element, drop.hoverclass);
    this.last_active = null;
  },

  activate: function(drop) {
    if(this.last_active) this.deactivate(this.last_active);
    if(drop.hoverclass)
      Element.Class.add(drop.element, drop.hoverclass);
    this.last_active = drop;
  },

  show: function(event, element) {
    if(!this.drops.length) return;
    var pX = Event.pointerX(event);
    var pY = Event.pointerY(event);
    Position.prepare();

    var i = this.drops.length-1; do {
      var drop = this.drops[i];
      if(this.isAffected(pX, pY, element, drop)) {
        if(drop.onHover)
           drop.onHover(element, drop.element, Position.overlap(drop.overlap, drop.element));
        if(drop.greedy) { 
          this.activate(drop);
          return;
        }
      }
    } while (i--);
    
    if(this.last_active) this.deactivate(this.last_active);
  },

  fire: function(event, element) {
    if(!this.last_active) return;
    Position.prepare();

    if (this.isAffected(Event.pointerX(event), Event.pointerY(event), element, this.last_active))
      if (this.last_active.onDrop) 
        this.last_active.onDrop(element, this.last_active.element, event);
  },

  reset: function() {
    if(this.last_active)
      this.deactivate(this.last_active);
  }
}

var Draggables = {
  observers: [],
  addObserver: function(observer) {
    this.observers.push(observer);    
  },
  removeObserver: function(element) {  // element instead of obsever fixes mem leaks
    this.observers = this.observers.reject( function(o) { return o.element==element });
  },
  notify: function(eventName, draggable) {  // 'onStart', 'onEnd'
    this.observers.invoke(eventName, draggable);
  }
}

/*--------------------------------------------------------------------------*/

var Draggable = Class.create();
Draggable.prototype = {
  initialize: function(element) {
    var options = Object.extend({
      handle: false,
      starteffect: function(element) { 
        new Effect.Opacity(element, {duration:0.2, from:1.0, to:0.7}); 
      },
      reverteffect: function(element, top_offset, left_offset) {
        var dur = Math.sqrt(Math.abs(top_offset^2)+Math.abs(left_offset^2))*0.02;
        new Effect.MoveBy(element, -top_offset, -left_offset, {duration:dur});
      },
      endeffect: function(element) { 
         new Effect.Opacity(element, {duration:0.2, from:0.7, to:1.0}); 
      },
      zindex: 1000,
      revert: false
    }, arguments[1] || {});

    this.element      = $(element);
    if(options.handle && (typeof options.handle == 'string'))
      this.handle = Element.Class.childrenWith(this.element, options.handle)[0];
      
    if(!this.handle) this.handle = $(options.handle);
    if(!this.handle) this.handle = this.element;

    Element.makePositioned(this.element); // fix IE    

    this.offsetX      = 0;
    this.offsetY      = 0;
    this.originalLeft = this.currentLeft();
    this.originalTop  = this.currentTop();
    this.originalX    = this.element.offsetLeft;
    this.originalY    = this.element.offsetTop;

    this.options      = options;

    this.active       = false;
    this.dragging     = false;   

    this.eventMouseDown = this.startDrag.bindAsEventListener(this);
    this.eventMouseUp   = this.endDrag.bindAsEventListener(this);
    this.eventMouseMove = this.update.bindAsEventListener(this);
    this.eventKeypress  = this.keyPress.bindAsEventListener(this);
    
    this.registerEvents();
  },
  destroy: function() {
    Event.stopObserving(this.handle, "mousedown", this.eventMouseDown);
    this.unregisterEvents();
  },
  registerEvents: function() {
    Event.observe(document, "mouseup", this.eventMouseUp);
    Event.observe(document, "mousemove", this.eventMouseMove);
    Event.observe(document, "keypress", this.eventKeypress);
    Event.observe(this.handle, "mousedown", this.eventMouseDown);
  },
  unregisterEvents: function() {
    //if(!this.active) return;
    //Event.stopObserving(document, "mouseup", this.eventMouseUp);
    //Event.stopObserving(document, "mousemove", this.eventMouseMove);
    //Event.stopObserving(document, "keypress", this.eventKeypress);
  },
  currentLeft: function() {
    return parseInt(this.element.style.left || '0');
  },
  currentTop: function() {
    return parseInt(this.element.style.top || '0')
  },
  startDrag: function(event) {
    if(Event.isLeftClick(event)) {
      
      // abort on form elements, fixes a Firefox issue
      var src = Event.element(event);
      if(src.tagName && (
        src.tagName=='INPUT' ||
        src.tagName=='SELECT' ||
        src.tagName=='BUTTON' ||
        src.tagName=='TEXTAREA')) return;
      
      // this.registerEvents();
      this.active = true;
      var pointer = [Event.pointerX(event), Event.pointerY(event)];
      var offsets = Position.cumulativeOffset(this.element);
      this.offsetX =  (pointer[0] - offsets[0]);
      this.offsetY =  (pointer[1] - offsets[1]);
      Event.stop(event);
    }
  },
  finishDrag: function(event, success) {
    // this.unregisterEvents();

    this.active = false;
    this.dragging = false;

    if(this.options.ghosting) {
      Position.relativize(this.element);
      Element.remove(this._clone);
      this._clone = null;
    }

    if(success) Droppables.fire(event, this.element);
    Draggables.notify('onEnd', this);

    var revert = this.options.revert;
    if(revert && typeof revert == 'function') revert = revert(this.element);

    if(revert && this.options.reverteffect) {
      this.options.reverteffect(this.element, 
      this.currentTop()-this.originalTop,
      this.currentLeft()-this.originalLeft);
    } else {
      this.originalLeft = this.currentLeft();
      this.originalTop  = this.currentTop();
    }

    if(this.options.zindex)
      this.element.style.zIndex = this.originalZ;

    if(this.options.endeffect) 
      this.options.endeffect(this.element);


    Droppables.reset();
  },
  keyPress: function(event) {
    if(this.active) {
      if(event.keyCode==Event.KEY_ESC) {
        this.finishDrag(event, false);
        Event.stop(event);
      }
    }
  },
  endDrag: function(event) {
    if(this.active && this.dragging) {
      this.finishDrag(event, true);
      Event.stop(event);
    }
    this.active = false;
    this.dragging = false;
  },
  draw: function(event) {
    var pointer = [Event.pointerX(event), Event.pointerY(event)];
    var offsets = Position.cumulativeOffset(this.element);
    offsets[0] -= this.currentLeft();
    offsets[1] -= this.currentTop();
    var style = this.element.style;
    if((!this.options.constraint) || (this.options.constraint=='horizontal'))
      style.left = (pointer[0] - offsets[0] - this.offsetX) + "px";
    if((!this.options.constraint) || (this.options.constraint=='vertical'))
      style.top  = (pointer[1] - offsets[1] - this.offsetY) + "px";
    if(style.visibility=="hidden") style.visibility = ""; // fix gecko rendering
  },
  update: function(event) {
   if(this.active) {
      if(!this.dragging) {
        var style = this.element.style;
        this.dragging = true;
        
        if(Element.getStyle(this.element,'position')=='') 
          style.position = "relative";
        
        if(this.options.zindex) {
          this.options.originalZ = parseInt(Element.getStyle(this.element,'z-index') || 0);
          style.zIndex = this.options.zindex;
        }

        if(this.options.ghosting) {
          this._clone = this.element.cloneNode(true);
          Position.absolutize(this.element);
          this.element.parentNode.insertBefore(this._clone, this.element);
        }

        Draggables.notify('onStart', this);
        if(this.options.starteffect) this.options.starteffect(this.element);
      }

      Droppables.show(event, this.element);
      this.draw(event);
      if(this.options.change) this.options.change(this);

      // fix AppleWebKit rendering
      if(navigator.appVersion.indexOf('AppleWebKit')>0) window.scrollBy(0,0); 

      Event.stop(event);
   }
  }
}

/*--------------------------------------------------------------------------*/

var SortableObserver = Class.create();
SortableObserver.prototype = {
  initialize: function(element, observer) {
    this.element   = $(element);
    this.observer  = observer;
    this.lastValue = Sortable.serialize(this.element);
  },
  onStart: function() {
    this.lastValue = Sortable.serialize(this.element);
  },
  onEnd: function() {
    Sortable.unmark();
    if(this.lastValue != Sortable.serialize(this.element))
      this.observer(this.element)
  }
}

var Sortable = {
  sortables: new Array(),
  options: function(element){
    element = $(element);
    return this.sortables.detect(function(s) { return s.element == element });
  },
  destroy: function(element){
    element = $(element);
    this.sortables.findAll(function(s) { return s.element == element }).each(function(s){
      Draggables.removeObserver(s.element);
      s.droppables.each(function(d){ Droppables.remove(d) });
      s.draggables.invoke('destroy');
    });
    this.sortables = this.sortables.reject(function(s) { return s.element == element });
  },
  create: function(element) {
    element = $(element);
    var options = Object.extend({ 
      element:     element,
      tag:         'li',       // assumes li children, override with tag: 'tagname'
      dropOnEmpty: false,
      tree:        false,      // fixme: unimplemented
      overlap:     'vertical', // one of 'vertical', 'horizontal'
      constraint:  'vertical', // one of 'vertical', 'horizontal', false
      containment: element,    // also takes array of elements (or id's); or false
      handle:      false,      // or a CSS class
      only:        false,
      hoverclass:  null,
      ghosting:    false,
      format:      null,
      onChange:    function() {},
      onUpdate:    function() {}
    }, arguments[1] || {});

    // clear any old sortable with same element
    this.destroy(element);

    // build options for the draggables
    var options_for_draggable = {
      revert:      true,
      ghosting:    options.ghosting,
      constraint:  options.constraint,
      handle:      options.handle };

    if(options.starteffect)
      options_for_draggable.starteffect = options.starteffect;

    if(options.reverteffect)
      options_for_draggable.reverteffect = options.reverteffect;
    else
      if(options.ghosting) options_for_draggable.reverteffect = function(element) {
        element.style.top  = 0;
        element.style.left = 0;
      };

    if(options.endeffect)
      options_for_draggable.endeffect = options.endeffect;

    if(options.zindex)
      options_for_draggable.zindex = options.zindex;

    // build options for the droppables  
    var options_for_droppable = {
      overlap:     options.overlap,
      containment: options.containment,
      hoverclass:  options.hoverclass,
      onHover:     Sortable.onHover,
      greedy:      !options.dropOnEmpty
    }

    // fix for gecko engine
    Element.cleanWhitespace(element); 

    options.draggables = [];
    options.droppables = [];

    // make it so

    // drop on empty handling
    if(options.dropOnEmpty) {
      Droppables.add(element,
        {containment: options.containment, onHover: Sortable.onEmptyHover, greedy: false});
      options.droppables.push(element);
    }

    (this.findElements(element, options) || []).each( function(e) {
      // handles are per-draggable
      var handle = options.handle ? 
        Element.Class.childrenWith(e, options.handle)[0] : e;    
      options.draggables.push(
        new Draggable(e, Object.extend(options_for_draggable, { handle: handle })));
      Droppables.add(e, options_for_droppable);
      options.droppables.push(e);      
    });

    // keep reference
    this.sortables.push(options);

    // for onupdate
    Draggables.addObserver(new SortableObserver(element, options.onUpdate));

  },

  // return all suitable-for-sortable elements in a guaranteed order
  findElements: function(element, options) {
    if(!element.hasChildNodes()) return null;
    var elements = [];
    $A(element.childNodes).each( function(e) {
      if(e.tagName && e.tagName==options.tag.toUpperCase() &&
        (!options.only || (Element.Class.has(e, options.only))))
          elements.push(e);
      if(options.tree) {
        var grandchildren = this.findElements(e, options);
        if(grandchildren) elements.push(grandchildren);
      }
    });

    return (elements.length>0 ? elements.flatten() : null);
  },

  onHover: function(element, dropon, overlap) {
    if(overlap>0.5) {
      Sortable.mark(dropon, 'before');
      if(dropon.previousSibling != element) {
        var oldParentNode = element.parentNode;
        element.style.visibility = "hidden"; // fix gecko rendering
        dropon.parentNode.insertBefore(element, dropon);
        if(dropon.parentNode!=oldParentNode) 
          Sortable.options(oldParentNode).onChange(element);
        Sortable.options(dropon.parentNode).onChange(element);
      }
    } else {
      Sortable.mark(dropon, 'after');
      var nextElement = dropon.nextSibling || null;
      if(nextElement != element) {
        var oldParentNode = element.parentNode;
        element.style.visibility = "hidden"; // fix gecko rendering
        dropon.parentNode.insertBefore(element, nextElement);
        if(dropon.parentNode!=oldParentNode) 
          Sortable.options(oldParentNode).onChange(element);
        Sortable.options(dropon.parentNode).onChange(element);
      }
    }
  },

  onEmptyHover: function(element, dropon) {
    if(element.parentNode!=dropon) {
      dropon.appendChild(element);
    }
  },

  unmark: function() {
    if(Sortable._marker) Element.hide(Sortable._marker);
  },

  mark: function(dropon, position) {
    // mark on ghosting only
    var sortable = Sortable.options(dropon.parentNode);
    if(sortable && !sortable.ghosting) return; 

    if(!Sortable._marker) {
      Sortable._marker = $('dropmarker') || document.createElement('DIV');
      Element.hide(Sortable._marker);
      Element.Class.add(Sortable._marker, 'dropmarker');
      Sortable._marker.style.position = 'absolute';
      document.getElementsByTagName("body").item(0).appendChild(Sortable._marker);
    }    
    var offsets = Position.cumulativeOffset(dropon);
    Sortable._marker.style.top  = offsets[1] + 'px';
    if(position=='after') Sortable._marker.style.top = (offsets[1]+dropon.clientHeight) + 'px';
    Sortable._marker.style.left = offsets[0] + 'px';
    Element.show(Sortable._marker);
  },

  serialize: function(element) {
    element = $(element);
    var sortableOptions = this.options(element);
    var options = Object.extend({
      tag:  sortableOptions.tag,
      only: sortableOptions.only,
      name: element.id,
      format: sortableOptions.format || /^[^_]*_(.*)$/
    }, arguments[1] || {});
    return $(this.findElements(element, options) || []).collect( function(item) {
      return (encodeURIComponent(options.name) + "[]=" + 
              encodeURIComponent(item.id.match(options.format) ? item.id.match(options.format)[1] : ''));
    }).join("&");
  }
} // Copyright (c) 2005 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
//           (c) 2005 Ivan Krstic (http://blogs.law.harvard.edu/ivan)
//           (c) 2005 Jon Tirsen (http://www.tirsen.com)
// Contributors:
//  Richard Livsey
//  Rahul Bhargava
//  Rob Wills
// 
// See scriptaculous.js for full license.

// Autocompleter.Base handles all the autocompletion functionality 
// that's independent of the data source for autocompletion. This
// includes drawing the autocompletion menu, observing keyboard
// and mouse events, and similar.
//
// Specific autocompleters need to provide, at the very least, 
// a getUpdatedChoices function that will be invoked every time
// the text inside the monitored textbox changes. This method 
// should get the text for which to provide autocompletion by
// invoking this.getToken(), NOT by directly accessing
// this.element.value. This is to allow incremental tokenized
// autocompletion. Specific auto-completion logic (AJAX, etc)
// belongs in getUpdatedChoices.
//
// Tokenized incremental autocompletion is enabled automatically
// when an autocompleter is instantiated with the 'tokens' option
// in the options parameter, e.g.:
// new Ajax.Autocompleter('id','upd', '/url/', { tokens: ',' });
// will incrementally autocomplete with a comma as the token.
// Additionally, ',' in the above example can be replaced with
// a token array, e.g. { tokens: [',', '\n'] } which
// enables autocompletion on multiple tokens. This is most 
// useful when one of the tokens is \n (a newline), as it 
// allows smart autocompletion after linebreaks.

var Autocompleter = {}
Autocompleter.Base = function() {};
Autocompleter.Base.prototype = {
  baseInitialize: function(element, update, options) {
    this.element     = $(element); 
    this.update      = $(update);  
    this.hasFocus    = false; 
    this.changed     = false; 
    this.active      = false; 
    this.index       = 0;     
    this.entryCount  = 0;

    if (this.setOptions)
      this.setOptions(options);
    else
      this.options = options || {};

    this.options.paramName    = this.options.paramName || this.element.name;
    this.options.tokens       = this.options.tokens || [];
    this.options.frequency    = this.options.frequency || 0.4;
    this.options.minChars     = this.options.minChars || 1;
    this.options.onShow       = this.options.onShow || 
    function(element, update){ 
      if(!update.style.position || update.style.position=='absolute') {
        update.style.position = 'absolute';
        Position.clone(element, update, {setHeight: false, offsetTop: element.offsetHeight});
      }
      Effect.Appear(update,{duration:0.15});
    };
    this.options.onHide = this.options.onHide || 
    function(element, update){ new Effect.Fade(update,{duration:0.15}) };

    if (typeof(this.options.tokens) == 'string') 
      this.options.tokens = new Array(this.options.tokens);

    this.observer = null;
    
    this.element.setAttribute('autocomplete','off');

    Element.hide(this.update);

    Event.observe(this.element, "blur", this.onBlur.bindAsEventListener(this));
    Event.observe(this.element, "keypress", this.onKeyPress.bindAsEventListener(this));
  },

  show: function() {
    if(Element.getStyle(this.update, 'display')=='none') this.options.onShow(this.element, this.update);
    if(!this.iefix && (navigator.appVersion.indexOf('MSIE')>0) && (Element.getStyle(this.update, 'position')=='absolute')) {
      new Insertion.After(this.update, 
       '<iframe id="' + this.update.id + '_iefix" '+
       'style="display:none;position:absolute;filter:progid:DXImageTransform.Microsoft.Alpha(opacity=0);" ' +
       'src="javascript:false;" frameborder="0" scrolling="no"></iframe>');
      this.iefix = $(this.update.id+'_iefix');
    }
    if(this.iefix) setTimeout(this.fixIEOverlapping.bind(this), 50);
  },
  
  fixIEOverlapping: function() {
    Position.clone(this.update, this.iefix);
    this.iefix.style.zIndex = 1;
    this.update.style.zIndex = 2;
    Element.show(this.iefix);
  },

  hide: function() {
    this.stopIndicator();
    if(Element.getStyle(this.update, 'display')!='none') this.options.onHide(this.element, this.update);
    if(this.iefix) Element.hide(this.iefix);
  },

  startIndicator: function() {
    if(this.options.indicator) Element.show(this.options.indicator);
  },

  stopIndicator: function() {
    if(this.options.indicator) Element.hide(this.options.indicator);
  },

  onKeyPress: function(event) {
    if(this.active)
      switch(event.keyCode) {
       case Event.KEY_TAB:
       case Event.KEY_RETURN:
         this.selectEntry();
         Event.stop(event);
       case Event.KEY_ESC:
         this.hide();
         this.active = false;
         Event.stop(event);
         return;
       case Event.KEY_LEFT:
       case Event.KEY_RIGHT:
         return;
       case Event.KEY_UP:
         this.markPrevious();
         this.render();
         if(navigator.appVersion.indexOf('AppleWebKit')>0) Event.stop(event);
         return;
       case Event.KEY_DOWN:
         this.markNext();
         this.render();
         if(navigator.appVersion.indexOf('AppleWebKit')>0) Event.stop(event);
         return;
      }
     else 
      if(event.keyCode==Event.KEY_TAB || event.keyCode==Event.KEY_RETURN) 
        return;

    this.changed = true;
    this.hasFocus = true;

    if(this.observer) clearTimeout(this.observer);
      this.observer = 
        setTimeout(this.onObserverEvent.bind(this), this.options.frequency*1000);
  },

  onHover: function(event) {
    var element = Event.findElement(event, 'LI');
    if(this.index != element.autocompleteIndex) 
    {
        this.index = element.autocompleteIndex;
        this.render();
    }
    Event.stop(event);
  },
  
  onClick: function(event) {
    var element = Event.findElement(event, 'LI');
    this.index = element.autocompleteIndex;
    this.selectEntry();
    this.hide();
  },
  
  onBlur: function(event) {
    // needed to make click events working
    setTimeout(this.hide.bind(this), 250);
    this.hasFocus = false;
    this.active = false;     
  }, 
  
  render: function() {
    if(this.entryCount > 0) {
      for (var i = 0; i < this.entryCount; i++)
        this.index==i ? 
          Element.addClassName(this.getEntry(i),"selected") : 
          Element.removeClassName(this.getEntry(i),"selected");
        
      if(this.hasFocus) { 
        this.show();
        this.active = true;
      }
    } else this.hide();
  },
  
  markPrevious: function() {
    if(this.index > 0) this.index--
      else this.index = this.entryCount-1;
  },
  
  markNext: function() {
    if(this.index < this.entryCount-1) this.index++
      else this.index = 0;
  },
  
  getEntry: function(index) {
    return this.update.firstChild.childNodes[index];
  },
  
  getCurrentEntry: function() {
    return this.getEntry(this.index);
  },
  
  selectEntry: function() {
    this.active = false;
    this.updateElement(this.getCurrentEntry());
  },

  updateElement: function(selectedElement) {
    if (this.options.updateElement) {
      this.options.updateElement(selectedElement);
      return;
    }

    var value = Element.collectTextNodesIgnoreClass(selectedElement, 'informal');
    var lastTokenPos = this.findLastToken();
    if (lastTokenPos != -1) {
      var newValue = this.element.value.substr(0, lastTokenPos + 1);
      var whitespace = this.element.value.substr(lastTokenPos + 1).match(/^\s+/);
      if (whitespace)
        newValue += whitespace[0];
      this.element.value = newValue + value;
    } else {
      this.element.value = value;
    }
    this.element.focus();
    
    if (this.options.afterUpdateElement)
      this.options.afterUpdateElement(this.element, selectedElement);
  },

  updateChoices: function(choices) {
    if(!this.changed && this.hasFocus) {
      this.update.innerHTML = choices;
      Element.cleanWhitespace(this.update);
      Element.cleanWhitespace(this.update.firstChild);

      if(this.update.firstChild && this.update.firstChild.childNodes) {
        this.entryCount = 
          this.update.firstChild.childNodes.length;
        for (var i = 0; i < this.entryCount; i++) {
          var entry = this.getEntry(i);
          entry.autocompleteIndex = i;
          this.addObservers(entry);
        }
      } else { 
        this.entryCount = 0;
      }

      this.stopIndicator();

      this.index = 0;
      this.render();
    }
  },

  addObservers: function(element) {
    Event.observe(element, "mouseover", this.onHover.bindAsEventListener(this));
    Event.observe(element, "click", this.onClick.bindAsEventListener(this));
  },

  onObserverEvent: function() {
    this.changed = false;   
    if(this.getToken().length>=this.options.minChars) {
      this.startIndicator();
      this.getUpdatedChoices();
    } else {
      this.active = false;
      this.hide();
    }
  },

  getToken: function() {
    var tokenPos = this.findLastToken();
    if (tokenPos != -1)
      var ret = this.element.value.substr(tokenPos + 1).replace(/^\s+/,'').replace(/\s+$/,'');
    else
      var ret = this.element.value;

    return /\n/.test(ret) ? '' : ret;
  },

  findLastToken: function() {
    var lastTokenPos = -1;

    for (var i=0; i<this.options.tokens.length; i++) {
      var thisTokenPos = this.element.value.lastIndexOf(this.options.tokens[i]);
      if (thisTokenPos > lastTokenPos)
        lastTokenPos = thisTokenPos;
    }
    return lastTokenPos;
  }
}

Ajax.Autocompleter = Class.create();
Object.extend(Object.extend(Ajax.Autocompleter.prototype, Autocompleter.Base.prototype), {
  initialize: function(element, update, url, options) {
	  this.baseInitialize(element, update, options);
    this.options.asynchronous  = true;
    this.options.onComplete    = this.onComplete.bind(this);
    this.options.defaultParams = this.options.parameters || null;
    this.url                   = url;
  },

  getUpdatedChoices: function() {
    entry = encodeURIComponent(this.options.paramName) + '=' + 
      encodeURIComponent(this.getToken());

    this.options.parameters = this.options.callback ?
      this.options.callback(this.element, entry) : entry;

    if(this.options.defaultParams) 
      this.options.parameters += '&' + this.options.defaultParams;

    new Ajax.Request(this.url, this.options);
  },

  onComplete: function(request) {
    this.updateChoices(request.responseText);
  }

});

// The local array autocompleter. Used when you'd prefer to
// inject an array of autocompletion options into the page, rather
// than sending out Ajax queries, which can be quite slow sometimes.
//
// The constructor takes four parameters. The first two are, as usual,
// the id of the monitored textbox, and id of the autocompletion menu.
// The third is the array you want to autocomplete from, and the fourth
// is the options block.
//
// Extra local autocompletion options:
// - choices - How many autocompletion choices to offer
//
// - partialSearch - If false, the autocompleter will match entered
//                    text only at the beginning of strings in the 
//                    autocomplete array. Defaults to true, which will
//                    match text at the beginning of any *word* in the
//                    strings in the autocomplete array. If you want to
//                    search anywhere in the string, additionally set
//                    the option fullSearch to true (default: off).
//
// - fullSsearch - Search anywhere in autocomplete array strings.
//
// - partialChars - How many characters to enter before triggering
//                   a partial match (unlike minChars, which defines
//                   how many characters are required to do any match
//                   at all). Defaults to 2.
//
// - ignoreCase - Whether to ignore case when autocompleting.
//                 Defaults to true.
//
// It's possible to pass in a custom function as the 'selector' 
// option, if you prefer to write your own autocompletion logic.
// In that case, the other options above will not apply unless
// you support them.

Autocompleter.Local = Class.create();
Autocompleter.Local.prototype = Object.extend(new Autocompleter.Base(), {
  initialize: function(element, update, array, options) {
    this.baseInitialize(element, update, options);
    this.options.array = array;
  },

  getUpdatedChoices: function() {
    this.updateChoices(this.options.selector(this));
  },

  setOptions: function(options) {
    this.options = Object.extend({
      choices: 10,
      partialSearch: true,
      partialChars: 2,
      ignoreCase: true,
      fullSearch: false,
      selector: function(instance) {
        var ret       = []; // Beginning matches
        var partial   = []; // Inside matches
        var entry     = instance.getToken();
        var count     = 0;

        for (var i = 0; i < instance.options.array.length &&  
          ret.length < instance.options.choices ; i++) { 

          var elem = instance.options.array[i];
          var foundPos = instance.options.ignoreCase ? 
            elem.toLowerCase().indexOf(entry.toLowerCase()) : 
            elem.indexOf(entry);

          while (foundPos != -1) {
            if (foundPos == 0 && elem.length != entry.length) { 
              ret.push("<li><strong>" + elem.substr(0, entry.length) + "</strong>" + 
                elem.substr(entry.length) + "</li>");
              break;
            } else if (entry.length >= instance.options.partialChars && 
              instance.options.partialSearch && foundPos != -1) {
              if (instance.options.fullSearch || /\s/.test(elem.substr(foundPos-1,1))) {
                partial.push("<li>" + elem.substr(0, foundPos) + "<strong>" +
                  elem.substr(foundPos, entry.length) + "</strong>" + elem.substr(
                  foundPos + entry.length) + "</li>");
                break;
              }
            }

            foundPos = instance.options.ignoreCase ? 
              elem.toLowerCase().indexOf(entry.toLowerCase(), foundPos + 1) : 
              elem.indexOf(entry, foundPos + 1);

          }
        }
        if (partial.length)
          ret = ret.concat(partial.slice(0, instance.options.choices - ret.length))
        return "<ul>" + ret.join('') + "</ul>";
      }
    }, options || {});
  }
});

// AJAX in-place editor
//
// see documentation on http://wiki.script.aculo.us/scriptaculous/show/Ajax.InPlaceEditor

Ajax.InPlaceEditor = Class.create();
Ajax.InPlaceEditor.defaultHighlightColor = "#FFFF99";
Ajax.InPlaceEditor.prototype = {
  initialize: function(element, url, options) {
    this.url = url;
    this.element = $(element);

    this.options = Object.extend({
      okText: "ok",
      cancelText: "cancel",
      savingText: "Saving...",
      clickToEditText: "Click to edit",
      okText: "ok",
      rows: 1,
      onComplete: function(transport, element) {
        new Effect.Highlight(element, {startcolor: this.options.highlightcolor});
      },
      onFailure: function(transport) {
        alert("Error communicating with the server: " + transport.responseText.stripTags());
      },
      callback: function(form) {
        return Form.serialize(form);
      },
      handleLineBreaks: true,
      loadingText: 'Loading...',
      savingClassName: 'inplaceeditor-saving',
      loadingClassName: 'inplaceeditor-loading',
      formClassName: 'inplaceeditor-form',
      highlightcolor: Ajax.InPlaceEditor.defaultHighlightColor,
      highlightendcolor: "#FFFFFF",
      externalControl:	null,
      ajaxOptions: {}
    }, options || {});

    if(!this.options.formId && this.element.id) {
      this.options.formId = this.element.id + "-inplaceeditor";
      if ($(this.options.formId)) {
        // there's already a form with that name, don't specify an id
        this.options.formId = null;
      }
    }
    
    if (this.options.externalControl) {
      this.options.externalControl = $(this.options.externalControl);
    }
    
    this.originalBackground = Element.getStyle(this.element, 'background-color');
    if (!this.originalBackground) {
      this.originalBackground = "transparent";
    }
    
    this.element.title = this.options.clickToEditText;
    
    this.onclickListener = this.enterEditMode.bindAsEventListener(this);
    this.mouseoverListener = this.enterHover.bindAsEventListener(this);
    this.mouseoutListener = this.leaveHover.bindAsEventListener(this);
    Event.observe(this.element, 'click', this.onclickListener);
    Event.observe(this.element, 'mouseover', this.mouseoverListener);
    Event.observe(this.element, 'mouseout', this.mouseoutListener);
    if (this.options.externalControl) {
      Event.observe(this.options.externalControl, 'click', this.onclickListener);
      Event.observe(this.options.externalControl, 'mouseover', this.mouseoverListener);
      Event.observe(this.options.externalControl, 'mouseout', this.mouseoutListener);
    }
  },
  enterEditMode: function() {
    if (this.saving) return;
    if (this.editing) return;
    this.editing = true;
    this.onEnterEditMode();
    if (this.options.externalControl) {
      Element.hide(this.options.externalControl);
    }
    Element.hide(this.element);
    this.createForm();
    this.element.parentNode.insertBefore(this.form, this.element);
    Field.focus(this.editField);
    // stop the event to avoid a page refresh in Safari
    if (arguments.length > 1) {
      Event.stop(arguments[0]);
    }
  },
  createForm: function() {
    this.form = document.createElement("form");
    this.form.id = this.options.formId;
    Element.addClassName(this.form, this.options.formClassName)
    this.form.onsubmit = this.onSubmit.bind(this);

    this.createEditField();

    if (this.options.textarea) {
      var br = document.createElement("br");
      this.form.appendChild(br);
    }

    okButton = document.createElement("input");
    okButton.type = "submit";
    okButton.value = this.options.okText;
    this.form.appendChild(okButton);

    cancelLink = document.createElement("a");
    cancelLink.href = "#";
    cancelLink.appendChild(document.createTextNode(this.options.cancelText));
    cancelLink.onclick = this.onclickCancel.bind(this);
    this.form.appendChild(cancelLink);
  },
  hasHTMLLineBreaks: function(string) {
    if (!this.options.handleLineBreaks) return false;
    return string.match(/<br/i) || string.match(/<p>/i);
  },
  convertHTMLLineBreaks: function(string) {
    return string.replace(/<br>/gi, "\n").replace(/<br\/>/gi, "\n").replace(/<\/p>/gi, "\n").replace(/<p>/gi, "");
  },
  createEditField: function() {
    var text;
    if(this.options.loadTextURL) {
      text = this.options.loadingText;
    } else {
      text = this.getText();
    }
    
    if (this.options.rows == 1 && !this.hasHTMLLineBreaks(text)) {
      this.options.textarea = false;
      var textField = document.createElement("input");
      textField.type = "text";
      textField.name = "value";
      textField.value = text;
      textField.style.backgroundColor = this.options.highlightcolor;
      var size = this.options.size || this.options.cols || 0;
      if (size != 0) textField.size = size;
      this.editField = textField;
    } else {
      this.options.textarea = true;
      var textArea = document.createElement("textarea");
      textArea.name = "value";
      textArea.value = this.convertHTMLLineBreaks(text);
      textArea.rows = this.options.rows;
      textArea.cols = this.options.cols || 40;
      this.editField = textArea;
    }
    
    if(this.options.loadTextURL) {
      this.loadExternalText();
    }
    this.form.appendChild(this.editField);
  },
  getText: function() {
    return this.element.innerHTML;
  },
  loadExternalText: function() {
    Element.addClassName(this.form, this.options.loadingClassName);
    this.editField.disabled = true;
    new Ajax.Request(
      this.options.loadTextURL,
      Object.extend({
        asynchronous: true,
        onComplete: this.onLoadedExternalText.bind(this)
      }, this.options.ajaxOptions)
    );
  },
  onLoadedExternalText: function(transport) {
    Element.removeClassName(this.form, this.options.loadingClassName);
    this.editField.disabled = false;
    this.editField.value = transport.responseText.stripTags();
  },
  onclickCancel: function() {
    this.onComplete();
    this.leaveEditMode();
    return false;
  },
  onFailure: function(transport) {
    this.options.onFailure(transport);
    if (this.oldInnerHTML) {
      this.element.innerHTML = this.oldInnerHTML;
      this.oldInnerHTML = null;
    }
    return false;
  },
  onSubmit: function() {
    // onLoading resets these so we need to save them away for the Ajax call
    var form = this.form;
    var value = this.editField.value;
    
    // do this first, sometimes the ajax call returns before we get a chance to switch on Saving...
    // which means this will actually switch on Saving... *after* we've left edit mode causing Saving...
    // to be displayed indefinitely
    this.onLoading();
    
    new Ajax.Updater(
      { 
        success: this.element,
         // don't update on failure (this could be an option)
        failure: null
      },
      this.url,
      Object.extend({
        parameters: this.options.callback(form, value),
        onComplete: this.onComplete.bind(this),
        onFailure: this.onFailure.bind(this)
      }, this.options.ajaxOptions)
    );
    // stop the event to avoid a page refresh in Safari
    if (arguments.length > 1) {
      Event.stop(arguments[0]);
    }
    return false;
  },
  onLoading: function() {
    this.saving = true;
    this.removeForm();
    this.leaveHover();
    this.showSaving();
  },
  showSaving: function() {
    this.oldInnerHTML = this.element.innerHTML;
    this.element.innerHTML = this.options.savingText;
    Element.addClassName(this.element, this.options.savingClassName);
    this.element.style.backgroundColor = this.originalBackground;
    Element.show(this.element);
  },
  removeForm: function() {
    if(this.form) {
      if (this.form.parentNode) Element.remove(this.form);
      this.form = null;
    }
  },
  enterHover: function() {
    if (this.saving) return;
    this.element.style.backgroundColor = this.options.highlightcolor;
    if (this.effect) {
      this.effect.cancel();
    }
    Element.addClassName(this.element, this.options.hoverClassName)
  },
  leaveHover: function() {
    if (this.options.backgroundColor) {
      this.element.style.backgroundColor = this.oldBackground;
    }
    Element.removeClassName(this.element, this.options.hoverClassName)
    if (this.saving) return;
    this.effect = new Effect.Highlight(this.element, {
      startcolor: this.options.highlightcolor,
      endcolor: this.options.highlightendcolor,
      restorecolor: this.originalBackground
    });
  },
  leaveEditMode: function() {
    Element.removeClassName(this.element, this.options.savingClassName);
    this.removeForm();
    this.leaveHover();
    this.element.style.backgroundColor = this.originalBackground;
    Element.show(this.element);
    if (this.options.externalControl) {
      Element.show(this.options.externalControl);
    }
    this.editing = false;
    this.saving = false;
    this.oldInnerHTML = null;
    this.onLeaveEditMode();
  },
  onComplete: function(transport) {
    this.leaveEditMode();
    this.options.onComplete.bind(this)(transport, this.element);
  },
  onEnterEditMode: function() {},
  onLeaveEditMode: function() {},
  dispose: function() {
    if (this.oldInnerHTML) {
      this.element.innerHTML = this.oldInnerHTML;
    }
    this.leaveEditMode();
    Event.stopObserving(this.element, 'click', this.onclickListener);
    Event.stopObserving(this.element, 'mouseover', this.mouseoverListener);
    Event.stopObserving(this.element, 'mouseout', this.mouseoutListener);
    if (this.options.externalControl) {
      Event.stopObserving(this.options.externalControl, 'click', this.onclickListener);
      Event.stopObserving(this.options.externalControl, 'mouseover', this.mouseoverListener);
      Event.stopObserving(this.options.externalControl, 'mouseout', this.mouseoutListener);
    }
  }
};// Copyright (c) 2005 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
//
// See scriptaculous.js for full license.


Object.debug = function(obj) {
  var info = [];
  
  if(typeof obj in ["string","number"]) {
    return obj;
  } else {
    for(property in obj)
      if(typeof obj[property]!="function")
        info.push(property + ' => ' + 
          (typeof obj[property] == "string" ?
            '"' + obj[property] + '"' :
            obj[property]));
  }
  
  return ("'" + obj + "' #" + typeof obj + 
    ": {" + info.join(", ") + "}");
}


String.prototype.toArray = function() {
  var results = [];
  for (var i = 0; i < this.length; i++)
    results.push(this.charAt(i));
  return results;
}

/*--------------------------------------------------------------------------*/

var Builder = {
  NODEMAP: {
    AREA: 'map',
    CAPTION: 'table',
    COL: 'table',
    COLGROUP: 'table',
    LEGEND: 'fieldset',
    OPTGROUP: 'select',
    OPTION: 'select',
    PARAM: 'object',
    TBODY: 'table',
    TD: 'table',
    TFOOT: 'table',
    TH: 'table',
    THEAD: 'table',
    TR: 'table'
  },
  // note: For Firefox < 1.5, OPTION and OPTGROUP tags are currently broken,
  //       due to a Firefox bug
  node: function(elementName) {
    elementName = elementName.toUpperCase();
    
    // try innerHTML approach
    var parentTag = this.NODEMAP[elementName] || 'div';
    var parentElement = document.createElement(parentTag);
    parentElement.innerHTML = "<" + elementName + "></" + elementName + ">";
    var element = parentElement.firstChild || null;
      
    // see if browser added wrapping tags
    if(element && (element.tagName != elementName))
      element = element.getElementsByTagName(elementName)[0];
    
    // fallback to createElement approach
    if(!element) element = document.createElement(elementName);
    
    // abort if nothing could be created
    if(!element) return;

    // attributes (or text)
    if(arguments[1])
      if(this._isStringOrNumber(arguments[1]) ||
        (arguments[1] instanceof Array)) {
          this._children(element, arguments[1]);
        } else {
          var attrs = this._attributes(arguments[1]);
          if(attrs.length) {
            parentElement.innerHTML = "<" +elementName + " " +
              attrs + "></" + elementName + ">";
            element = parentElement.firstChild || null;
            // workaround firefox 1.0.X bug
            if(!element) {
              element = document.createElement(elementName);
              for(attr in arguments[1]) 
                element[attr == 'class' ? 'className' : attr] = arguments[1][attr];
            }
            if(element.tagName != elementName)
              element = parentElement.getElementsByTagName(elementName)[0];
            }
        } 

    // text, or array of children
    if(arguments[2])
      this._children(element, arguments[2]);

     return element;
  },
  _text: function(text) {
     return document.createTextNode(text);
  },
  _attributes: function(attributes) {
    var attrs = [];
    for(attribute in attributes)
      attrs.push((attribute=='className' ? 'class' : attribute) +
          '="' + attributes[attribute].toString().escapeHTML() + '"');
    return attrs.join(" ");
  },
  _children: function(element, children) {
    if(typeof children=='object') { // array can hold nodes and text
      children.flatten().each( function(e) {
        if(typeof e=='object')
          element.appendChild(e)
        else
          if(Builder._isStringOrNumber(e))
            element.appendChild(Builder._text(e));
      });
    } else
      if(Builder._isStringOrNumber(children)) 
         element.appendChild(Builder._text(children));
  },
  _isStringOrNumber: function(param) {
    return(typeof param=='string' || typeof param=='number');
  }
}

/* ------------- element ext -------------- */

// adapted from http://dhtmlkitchen.com/learn/js/setstyle/index4.jsp
// note: Safari return null on elements with display:none; see http://bugzilla.opendarwin.org/show_bug.cgi?id=4125
// instead of "auto" values returns null so it's easier to use with || constructs

String.prototype.camelize = function() {
  var oStringList = this.split('-');
  if(oStringList.length == 1)    
    return oStringList[0];
  var ret = this.indexOf("-") == 0 ? 
    oStringList[0].charAt(0).toUpperCase() + oStringList[0].substring(1) : oStringList[0];
  for(var i = 1, len = oStringList.length; i < len; i++){
    var s = oStringList[i];
    ret += s.charAt(0).toUpperCase() + s.substring(1)
  }
  return ret;
}

Element.getStyle = function(element, style) {
  element = $(element);
  var value = element.style[style.camelize()];
  if(!value)
    if(document.defaultView && document.defaultView.getComputedStyle) {
      var css = document.defaultView.getComputedStyle(element, null);
      value = (css!=null) ? css.getPropertyValue(style) : null;
    } else if(element.currentStyle) {
      value = element.currentStyle[style.camelize()];
    }
  
  // If top, left, bottom, or right values have been queried, return "auto" for consistency resaons 
  // if position is "static", as Opera (and others?) returns the pixel values relative to root element 
  // (or positioning context?)
  if (window.opera && (style == "left" || style == "top" || style == "right" || style == "bottom"))
    if (Element.getStyle(element, "position") == "static") value = "auto";
    
  if(value=='auto') value = null;
  return value;
}

// converts rgb() and #xxx to #xxxxxx format,
// returns self (or first argument) if not convertable
String.prototype.parseColor = function() {
  color = "#";
  if(this.slice(0,4) == "rgb(") {
    var cols = this.slice(4,this.length-1).split(',');
    var i=0; do { color += parseInt(cols[i]).toColorPart() } while (++i<3);
  } else {
    if(this.slice(0,1) == '#') {
      if(this.length==4) for(var i=1;i<4;i++) color += (this.charAt(i) + this.charAt(i)).toLowerCase();
      if(this.length==7) color = this.toLowerCase();
    }
  }
  return(color.length==7 ? color : (arguments[0] || this));
}

Element.makePositioned = function(element) {
  element = $(element);
  var pos = Element.getStyle(element, 'position');
  if(pos =='static' || !pos) {
    element._madePositioned = true;
    element.style.position = "relative";
    // Opera returns the offset relative to the positioning context, when an element is position relative 
    // but top and left have not been defined
    if (window.opera){
      element.style.top = 0;
      element.style.left = 0;
    }  
  }
}
  
Element.undoPositioned = function(element) {
  element = $(element);
  if(typeof element._madePositioned != "undefined"){
    element._madePositioned = undefined;
    element.style.position = "";
    element.style.top = "";
    element.style.left = "";
    element.style.bottom = "";
    element.style.right = "";	  
  }
}

Element.makeClipping = function(element) {
  element = $(element);
  if (typeof element._overflow != 'undefined') return;
  element._overflow = element.style.overflow;
  if((Element.getStyle(element, 'overflow') || 'visible') != 'hidden') element.style.overflow = 'hidden';
}

Element.undoClipping = function(element) {
  element = $(element);
  if (typeof element._overflow == 'undefined') return;
  element.style.overflow = element._overflow;
  element._overflow = undefined;
}

Element.collectTextNodesIgnoreClass = function(element, ignoreclass) {
  var children = $(element).childNodes;
  var text     = "";
  var classtest = new RegExp("^([^ ]+ )*" + ignoreclass+ "( [^ ]+)*$","i");

  for (var i = 0; i < children.length; i++) {
    if(children[i].nodeType==3) {
      text+=children[i].nodeValue;
    } else {
      if((!children[i].className.match(classtest)) && children[i].hasChildNodes())
        text += Element.collectTextNodesIgnoreClass(children[i], ignoreclass);
    }
  }

  return text;
}

Element.setContentZoom = function(element, percent) {
  element = $(element);
  element.style.fontSize = (percent/100) + "em";  
  if(navigator.appVersion.indexOf('AppleWebKit')>0) window.scrollBy(0,0);
}

Element.getOpacity = function(element){
  var opacity;
  if (opacity = Element.getStyle(element, "opacity"))
    return parseFloat(opacity);
  if (opacity = (Element.getStyle(element, "filter") || '').match(/alpha\(opacity=(.*)\)/))
    if(opacity[1]) return parseFloat(opacity[1]) / 100;
  return 1.0;
}

Element.setOpacity = function(element, value){
  element= $(element);
  var els = element.style;
  if (value == 1){
    els.opacity = '0.999999';
    if(/MSIE/.test(navigator.userAgent))
      els.filter = Element.getStyle(element,'filter').replace(/alpha\([^\)]*\)/gi,'');
  } else {
    if(value < 0.00001) value = 0;
    els.opacity = value;
    if(/MSIE/.test(navigator.userAgent))
      els.filter = Element.getStyle(element,'filter').replace(/alpha\([^\)]*\)/gi,'') + 
        "alpha(opacity="+value*100+")";
  }  
}

Element.getInlineOpacity = function(element){
  element= $(element);
  var op;
  op = element.style.opacity;
  if (typeof op != "undefined" && op != "") return op;
  return "";
}

Element.setInlineOpacity = function(element, value){
  element= $(element);
  var els = element.style;
  els.opacity = value;
}

Element.getDimensions = function(element){
  element = $(element);
  // All *Width and *Height properties give 0 on elements with display "none",
  // so enable the element temporarily
  if (Element.getStyle(element,'display') == "none"){
    var els = element.style;
    var originalVisibility = els.visibility;
    var originalPosition = els.position;
    els.visibility = "hidden";
    els.position = "absolute";
    els.display = "";
    var originalWidth = element.clientWidth;
    var originalHeight = element.clientHeight;
    els.display = "none";
    els.position = originalPosition;
    els.visibility = originalVisibility;
    return {width: originalWidth, height: originalHeight};    
  }
  
  return {width: element.offsetWidth, height: element.offsetHeight};
} 

/*--------------------------------------------------------------------------*/

Position.positionedOffset = function(element) {
  var valueT = 0, valueL = 0;
  do {
    valueT += element.offsetTop  || 0;
    valueL += element.offsetLeft || 0;
    element = element.offsetParent;
    if (element) {
      p = Element.getStyle(element,'position');
      if(p == 'relative' || p == 'absolute') break;
    }
  } while (element);
  return [valueL, valueT];
}

// Safari returns margins on body which is incorrect if the child is absolutely positioned.
// for performance reasons, we create a specialized version of Position.cumulativeOffset for
// KHTML/WebKit only

if(/Konqueror|Safari|KHTML/.test(navigator.userAgent)) {
  Position.cumulativeOffset = function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      
      if (element.offsetParent==document.body) 
        if (Element.getStyle(element,'position')=='absolute') break;
        
      element = element.offsetParent;
    } while (element);
    return [valueL, valueT];
  }
}

Position.page = function(forElement) {
  var valueT = 0, valueL = 0;

  var element = forElement;
  do {
    valueT += element.offsetTop  || 0;
    valueL += element.offsetLeft || 0;

    // Safari fix
    if (element.offsetParent==document.body)
      if (Element.getStyle(element,'position')=='absolute') break;
      
  } while (element = element.offsetParent);

  element = forElement;
  do {
    valueT -= element.scrollTop  || 0;
    valueL -= element.scrollLeft || 0;    
  } while (element = element.parentNode);

  return [valueL, valueT];
}

// elements with display:none don't return an offsetParent, 
// fall back to  manual calculation
Position.offsetParent = function(element) {
  if(element.offsetParent) return element.offsetParent;
  if(element == document.body) return element;
  
  while ((element = element.parentNode) && element != document.body)
    if (Element.getStyle(element,'position')!='static')
      return element;
  
  return document.body;
}

Position.clone = function(source, target) {
  var options = Object.extend({
    setLeft:    true,
    setTop:     true,
    setWidth:   true,
    setHeight:  true,
    offsetTop:  0,
    offsetLeft: 0
  }, arguments[2] || {})
  
  // find page position of source
  source = $(source);
  var p = Position.page(source);

  // find coordinate system to use
  target = $(target);
  var delta = [0, 0];
  var parent = null;
  // delta [0,0] will do fine with position: fixed elements, 
  // position:absolute needs offsetParent deltas
  if (Element.getStyle(target,'position') == 'absolute') {
    parent = Position.offsetParent(target);
    delta = Position.page(parent);
  }
  
  // correct by body offsets (fixes Safari)
  if (parent==document.body) {
    delta[0] -= document.body.offsetLeft;
    delta[1] -= document.body.offsetTop; 
  }

  // set position
  if(options.setLeft)   target.style.left  = (p[0] - delta[0] + options.offsetLeft) + "px";
  if(options.setTop)    target.style.top   = (p[1] - delta[1] + options.offsetTop) + "px";
  if(options.setWidth)  target.style.width = source.offsetWidth + "px";
  if(options.setHeight) target.style.height = source.offsetHeight + "px";
}

Position.absolutize = function(element) {
  element = $(element);
  if(element.style.position=='absolute') return;
  Position.prepare();

  var offsets = Position.positionedOffset(element);
  var top     = offsets[1];
  var left    = offsets[0];
  var width   = element.clientWidth;
  var height  = element.clientHeight;

  element._originalLeft   = left - parseFloat(element.style.left  || 0);
  element._originalTop    = top  - parseFloat(element.style.top || 0);
  element._originalWidth  = element.style.width;
  element._originalHeight = element.style.height;

  element.style.position = 'absolute';
  element.style.top    = top + 'px';;
  element.style.left   = left + 'px';;
  element.style.width  = width + 'px';;
  element.style.height = height + 'px';;
}

Position.relativize = function(element) {
  element = $(element);
  if(element.style.position=='relative') return;
  Position.prepare();

  element.style.position = 'relative';
  var top  = parseFloat(element.style.top  || 0) - (element._originalTop || 0);
  var left = parseFloat(element.style.left || 0) - (element._originalLeft || 0);

  element.style.top    = top + 'px';
  element.style.left   = left + 'px';
  element.style.height = element._originalHeight;
  element.style.width  = element._originalWidth;
}

/*--------------------------------------------------------------------------*/

Element.Class = {
    // Element.toggleClass(element, className) toggles the class being on/off
    // Element.toggleClass(element, className1, className2) toggles between both classes,
    //   defaulting to className1 if neither exist
    toggle: function(element, className) {
      if(Element.Class.has(element, className)) {
        Element.Class.remove(element, className);
        if(arguments.length == 3) Element.Class.add(element, arguments[2]);
      } else {
        Element.Class.add(element, className);
        if(arguments.length == 3) Element.Class.remove(element, arguments[2]);
      }
    },

    // gets space-delimited classnames of an element as an array
    get: function(element) {
      return $(element).className.split(' ');
    },

    // functions adapted from original functions by Gavin Kistner
    remove: function(element) {
      element = $(element);
      var removeClasses = arguments;
      $R(1,arguments.length-1).each( function(index) {
        element.className = 
          element.className.split(' ').reject( 
            function(klass) { return (klass == removeClasses[index]) } ).join(' ');
      });
    },

    add: function(element) {
      element = $(element);
      for(var i = 1; i < arguments.length; i++) {
        Element.Class.remove(element, arguments[i]);
        element.className += (element.className.length > 0 ? ' ' : '') + arguments[i];
      }
    },

    // returns true if all given classes exist in said element
    has: function(element) {
      element = $(element);
      if(!element || !element.className) return false;
      var regEx;
      for(var i = 1; i < arguments.length; i++) {
        if((typeof arguments[i] == 'object') && 
          (arguments[i].constructor == Array)) {
          for(var j = 0; j < arguments[i].length; j++) {
            regEx = new RegExp("(^|\\s)" + arguments[i][j] + "(\\s|$)");
            if(!regEx.test(element.className)) return false;
          }
        } else {
          regEx = new RegExp("(^|\\s)" + arguments[i] + "(\\s|$)");
          if(!regEx.test(element.className)) return false;
        }
      }
      return true;
    },

    // expects arrays of strings and/or strings as optional paramters
    // Element.Class.has_any(element, ['classA','classB','classC'], 'classD')
    has_any: function(element) {
      element = $(element);
      if(!element || !element.className) return false;
      var regEx;
      for(var i = 1; i < arguments.length; i++) {
        if((typeof arguments[i] == 'object') && 
          (arguments[i].constructor == Array)) {
          for(var j = 0; j < arguments[i].length; j++) {
            regEx = new RegExp("(^|\\s)" + arguments[i][j] + "(\\s|$)");
            if(regEx.test(element.className)) return true;
          }
        } else {
          regEx = new RegExp("(^|\\s)" + arguments[i] + "(\\s|$)");
          if(regEx.test(element.className)) return true;
        }
      }
      return false;
    },

    childrenWith: function(element, className) {
      var children = $(element).getElementsByTagName('*');
      var elements = new Array();

      for (var i = 0; i < children.length; i++)
        if (Element.Class.has(children[i], className))
          elements.push(children[i]);

      return elements;
    }
}