var Hiori = {};
Hiori.init = function() {
  ezpf.ui.showFullScreenAnnounce = function() {};
  Hiori.translations = {};
  Hiori.loadTranslations();
  Hiori.modifySceneManager();
};

Hiori.loadTranslations = function() {
  messages.forEach(msg => {
    Hiori.translations[msg.jp] = msg.tl;
  });
};

Hiori.modifySceneManager = function() {
  // Override the scene manager's replaceScene() function so we can detect when its fired
  if (window.aoba.sceneManager._sc3_replaceScene) return;//onPageFinished called twice!
  window.aoba.sceneManager._sc3_replaceScene = window.aoba.sceneManager.replaceScene;
  window.aoba.sceneManager.replaceScene = function(...replaceSceneArgs){
    // Override the addChild method based on the scene, each one has a different layer hierarchy
    if (replaceSceneArgs[0].auditionSceneName == 'produceAudition') {
      Hiori.overrideAddChild(replaceSceneArgs[0].children[0]);
    } else {
      Hiori.overrideAddChild(replaceSceneArgs[0]);
    }
    this._sc3_replaceScene(...replaceSceneArgs);
  };
};

Hiori.overrideAddChild = function(scene) {
  // Override the addChild() function so we know when a new one is spawned
  if (scene._sc3_addChild) return;
  scene._sc3_addChild = scene.addChild;
  scene.addChild = function(...addChildArgs){
    Hiori.findDialogFromScene(addChildArgs[0]);
    this._sc3_addChild(...addChildArgs);
  };
};

Hiori.findDialogFromScene = function(scene) {
  // Regular dialog
  if (scene._eventTracks) {
    if (scene._eventTracks.length) {
      // Event tracks has contents, translate it
      Hiori.translate(scene._eventTracks);
    } else {
      // Event tracks are not yet available, wait for its child to be added
      Hiori.overrideAddChild(scene);
    }
  }
  // Event dialogs
  if (scene._trackManager && scene._trackManager._tracks)
    Hiori.translate(scene._trackManager._tracks);
}

Hiori.translate = function(dialogList) {
  // Show raw texts in JSON form for data extraction
  var showRaws = showRaws = {
    // notes: [
    //   'Only translate from "jp" -> "tl". Untranslated character names are to be added separately.',
    // ],
    messages: dialogList.filter(v=>!!v.text).map(dialog => {
      return {
        ch: this.translations[dialog.speaker] || dialog.speaker,
        jp: dialog.text || dialog.select,
        tl: ''
      }
    })
  };
  console.log(JSON.stringify(showRaws, null, 2));

  // Translate the full dialog event
  return dialogList.map(dialog => {
    // Translate speakr
    if (dialog.speaker) {
      if (Hiori.translations[dialog.speaker]) {
        dialog.speaker = Hiori.translations[dialog.speaker];
      } else {
        console.log(dialog.speaker);
        // dialog.speaker = '+' + dialog.speaker + '+';
      }
    }
    // Translate message
    if (dialog.text) {
      if (Hiori.translations[dialog.text]) {
        dialog.text = Hiori.translations[dialog.text];
      } else {
        console.log(dialog.text);
        // dialog.text = '+' + dialog.text + '+';
      }
    }

    if (dialog.select) {
      if (Hiori.translations[dialog.select]) {
        dialog.select = Hiori.translations[dialog.select];
      } else {
        console.log(dialog.select);
      }
    }
  })
}

var tempMessages = {};
var messages = [];