# 🔊 Sound Setup Instructions

## 📁 File Location

The sound files need to be moved to the **`assets/sounds/`** folder for the SoundManager to work properly.

### Current Location:
```
Mobile/app/src/main/res/sounds/
├── backgroundgamemusic.mp3
├── success_bell-6776.mp3
└── wrong_answer.mp3
```

### Required Location:
```
Mobile/app/src/main/assets/sounds/
├── backgroundgamemusic.mp3
├── success_bell-6776.mp3
└── wrong_answer.mp3
```

## 🔧 Setup Steps

1. **Create the assets folder structure:**
   ```
   Mobile/app/src/main/assets/sounds/
   ```

2. **Move the sound files:**
   - Copy all 3 MP3 files from `res/sounds/` to `assets/sounds/`
   - Keep the same filenames

3. **Verify the structure:**
   ```
   Mobile/app/src/main/
   ├── assets/
   │   └── sounds/
   │       ├── backgroundgamemusic.mp3
   │       ├── success_bell-6776.mp3
   │       └── wrong_answer.mp3
   └── res/
       └── sounds/ (can be removed after moving)
   ```

## ✅ After Setup

The SoundManager will automatically:
- ✅ Play background music (loops continuously at 30% volume)
- ✅ Play success bell sound on correct answers
- ✅ Play error sound on wrong answers
- ✅ Handle pause/resume during app lifecycle
- ✅ Clean up resources properly

## 🎮 Integration

The SoundManager is already integrated into all game activities:
- PictureQuizActivity
- AudioIdentificationActivity
- SequencingCardsActivity
- EmotionRecognitionActivity
- PictureLabelingActivity

## 📝 Notes

- Background music volume: 30% (adjustable)
- Sound effects volume: 70% (adjustable)
- Music loops automatically
- Sounds can be enabled/disabled via SoundManager settings

