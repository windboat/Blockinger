/*
 * Copyright 2013 Simon Willeke
 * contact: hamstercount@hotmail.com
 */

/*
    This file is part of Blockinger.

    Blockinger is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Blockinger is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Blockinger.  If not, see <http://www.gnu.org/licenses/>.

    Diese Datei ist Teil von Blockinger.

    Blockinger ist Freie Software: Sie können es unter den Bedingungen
    der GNU General Public License, wie von der Free Software Foundation,
    Version 3 der Lizenz oder (nach Ihrer Option) jeder späteren
    veröffentlichten Version, weiterverbreiten und/oder modifizieren.

    Blockinger wird in der Hoffnung, dass es nützlich sein wird, aber
    OHNE JEDE GEWÄHELEISTUNG, bereitgestellt; sogar ohne die implizite
    Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
    Siehe die GNU General Public License für weitere Details.

    Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
    Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 */

package org.blockinger.game.components;

import android.R.color;
import android.preference.PreferenceManager;
import android.util.Log;

import org.blockinger.game.PieceGenerator;
import org.blockinger.game.R;
import org.blockinger.game.Square;
import org.blockinger.game.activities.GameActivity;
import org.blockinger.game.pieces.IPiece;
import org.blockinger.game.pieces.JPiece;
import org.blockinger.game.pieces.LPiece;
import org.blockinger.game.pieces.OPiece;
import org.blockinger.game.pieces.Piece;
import org.blockinger.game.pieces.SPiece;
import org.blockinger.game.pieces.TPiece;
import org.blockinger.game.pieces.ZPiece;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;


public class GameState extends Component implements Serializable {

    private static final long serialVersionUID = 201807191943L;

    public final static int state_startable = 0;
    public final static int state_running = 1;
    public final static int state_paused = 2;
    public final static int state_finished = 3;

    private static GameState instance;

    // References
    private transient PieceGenerator rng;
    public transient Board board;
    private transient GregorianCalendar date;
    private transient SimpleDateFormat formatter;
    public int hourOffset;

    // Game State
    private String playerName;
    private int activeIndex, previewIndex;
    private transient Piece[] activePieces;
    private transient Piece[] previewPieces;
    private boolean scheduleSpawn;
    private long spawnTime;
    //private boolean paused;
    //private boolean restartMe;
    private int stateOfTheGame;
    private long score;
    //private long consecutiveBonusScore;
    private int clearedLines;
    private int level;
    private int maxLevel;
    private long gameTime;     // += (systemtime - currenttime) at start of cycle
    private long currentTime;  // = systemtime at start of cycle
    private long nextDropTime;
    private long nextPlayerDropTime;
    private long nextPlayerMoveTime;
    private int[] dropIntervals; // =(1/gamespeed)
    private long playerDropInterval;
    private long playerMoveInterval;
    private int singleLineScore;
    private int doubleLineScore;
    private int trippleLineScore;
    private int multiTetrisScore;
    private boolean multitetris;
    private int quadLineScore;
    private int hardDropBonus;
    private int softDropBonus;
    private int spawn_delay;
    private int piece_start_x;
    private long actions;
    private int songtime;

    private long popupTime;
    private String popupString;
    private int popupAttack;
    private int popupSustain;
    private int popupDecay;
    private int softDropDistance;

    private boolean infinity = false;

	private GameState(GameActivity ga) {
		super(ga);
		actions = 0;
		board = new Board(host);
		date = new GregorianCalendar();
		formatter = new SimpleDateFormat("HH:mm:ss",Locale.US);
		date.setTimeInMillis(60000);
		if(formatter.format(date.getTime()).startsWith("23"))
			hourOffset = 1;
		else if(formatter.format(date.getTime()).startsWith("01"))
			hourOffset = -1;
		else
			hourOffset = 0;

		dropIntervals = host.getResources().getIntArray(R.array.intervals);
		singleLineScore = host.getResources().getInteger(R.integer.singleLineScore);
		doubleLineScore = host.getResources().getInteger(R.integer.doubleLineScore);
		trippleLineScore = host.getResources().getInteger(R.integer.trippleLineScore);
		multiTetrisScore = host.getResources().getInteger(R.integer.multiTetrisScore);
		quadLineScore = host.getResources().getInteger(R.integer.quadLineScore);
		hardDropBonus = host.getResources().getInteger(R.integer.hardDropBonus);
		softDropBonus = host.getResources().getInteger(R.integer.softDropBonus);
		softDropDistance = 0;
		spawn_delay = host.getResources().getInteger(R.integer.spawn_delay);
		piece_start_x = host.getResources().getInteger(R.integer.piece_start_x);
		popupAttack = host.getResources().getInteger(R.integer.popup_attack);
		popupSustain = host.getResources().getInteger(R.integer.popup_sustain);
		popupDecay = host.getResources().getInteger(R.integer.popup_decay);
		popupString = "";
		popupTime = -(popupAttack + popupSustain + popupDecay);
		clearedLines = 0;
		level = 0;
		score = 0;
		songtime = 0;
		maxLevel = host.getResources().getInteger(R.integer.levels);

		nextDropTime = host.getResources().getIntArray(R.array.intervals)[0];
		
		playerDropInterval = (int)(1000.0f / PreferenceManager.getDefaultSharedPreferences(host).getInt("pref_softdropspeed", 60));
		playerMoveInterval = (int)(1000.0f / PreferenceManager.getDefaultSharedPreferences(host).getInt("pref_movespeed", 60));
		nextPlayerDropTime = (int)(1000.0f / PreferenceManager.getDefaultSharedPreferences(host).getInt("pref_softdropspeed", 60));
		nextPlayerMoveTime = (int)(1000.0f / PreferenceManager.getDefaultSharedPreferences(host).getInt("pref_movespeed", 60));
		
		gameTime = 0;
		if(PreferenceManager.getDefaultSharedPreferences(host).getString("pref_rng", "sevenbag").equals("sevenbag") ||
				PreferenceManager.getDefaultSharedPreferences(host).getString("pref_rng", "7-Bag-Randomization (default)").equals("7-Bag-Randomization (default)"))
			rng = new PieceGenerator(PieceGenerator.STRAT_7BAG);
		else
			rng = new PieceGenerator(PieceGenerator.STRAT_RANDOM);
		
		// Initialize Pieces
		activePieces  = new Piece[7];
		previewPieces = new Piece[7];
		
		activePieces[0] = new IPiece(host);
		activePieces[1] = new JPiece(host);
		activePieces[2] = new LPiece(host);
		activePieces[3] = new OPiece(host);
		activePieces[4] = new SPiece(host);
		activePieces[5] = new TPiece(host);
		activePieces[6] = new ZPiece(host);
		
		previewPieces[0] = new IPiece(host);
		previewPieces[1] = new JPiece(host);
		previewPieces[2] = new LPiece(host);
		previewPieces[3] = new OPiece(host);
		previewPieces[4] = new SPiece(host);
		previewPieces[5] = new TPiece(host);
		previewPieces[6] = new ZPiece(host);
		
		// starting pieces
		activeIndex  = rng.next();
		previewIndex = rng.next();
		activePieces[activeIndex].setActive(true);

		//paused = true;
		//restartMe = false;
		stateOfTheGame = state_startable;
		scheduleSpawn = false;
		spawnTime = 0;
	}

    private GameState(GameActivity ga, GameStateProxy gameStateProxy) {
        super(ga);
        actions = gameStateProxy.actions;
        board = new Board(host);
        int width = board.getWidth();
        int height = board.getHeight();
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                int type = gameStateProxy.board[i][j];
                if(type == -1)
                    continue;

                board.set(i,j, new Square(type, ga));
            }
        }


        date = new GregorianCalendar();
        formatter = new SimpleDateFormat("HH:mm:ss",Locale.US);
        date.setTimeInMillis(60000);
        if(formatter.format(date.getTime()).startsWith("23"))
            hourOffset = 1;
        else if(formatter.format(date.getTime()).startsWith("01"))
            hourOffset = -1;
        else
            hourOffset = 0;

        dropIntervals = gameStateProxy.dropIntervals;
        singleLineScore = gameStateProxy.singleLineScore;
        doubleLineScore = gameStateProxy.doubleLineScore;
        trippleLineScore = gameStateProxy.trippleLineScore;
        multiTetrisScore = gameStateProxy.multiTetrisScore;
        quadLineScore = gameStateProxy.quadLineScore;
        hardDropBonus = gameStateProxy.hardDropBonus;
        softDropBonus = gameStateProxy.softDropBonus;
        softDropDistance = gameStateProxy.softDropDistance;
        spawn_delay = gameStateProxy.spawn_delay;
        piece_start_x = gameStateProxy.piece_start_x;
        popupAttack = gameStateProxy.popupAttack;
        popupSustain = gameStateProxy.popupSustain;
        popupDecay = gameStateProxy.popupDecay;
        popupString = gameStateProxy.popupString;
        popupTime = gameStateProxy.popupTime;
        clearedLines = gameStateProxy.clearedLines;
        level = gameStateProxy.level;
        score = gameStateProxy.score;
        songtime = gameStateProxy.songtime;
        maxLevel = gameStateProxy.maxLevel;

        nextDropTime = gameStateProxy.nextDropTime;

        playerDropInterval = gameStateProxy.playerDropInterval;
        playerMoveInterval = gameStateProxy.playerMoveInterval;
        nextPlayerDropTime = gameStateProxy.nextPlayerDropTime;
        nextPlayerMoveTime = gameStateProxy.nextPlayerMoveTime;

        gameTime = gameStateProxy.gameTime;
        if (PreferenceManager.getDefaultSharedPreferences(host).getString("pref_rng", "sevenbag").equals("sevenbag") ||
                PreferenceManager.getDefaultSharedPreferences(host).getString("pref_rng", "7-Bag-Randomization (default)").equals("7-Bag-Randomization (default)"))
            rng = new PieceGenerator(PieceGenerator.STRAT_7BAG);
        else
            rng = new PieceGenerator(PieceGenerator.STRAT_RANDOM);

        // Initialize Pieces
        activePieces = new Piece[7];
        previewPieces = new Piece[7];

        activePieces[0] = new IPiece(host);
        activePieces[1] = new JPiece(host);
        activePieces[2] = new LPiece(host);
        activePieces[3] = new OPiece(host);
        activePieces[4] = new SPiece(host);
        activePieces[5] = new TPiece(host);
        activePieces[6] = new ZPiece(host);

        previewPieces[0] = new IPiece(host);
        previewPieces[1] = new JPiece(host);
        previewPieces[2] = new LPiece(host);
        previewPieces[3] = new OPiece(host);
        previewPieces[4] = new SPiece(host);
        previewPieces[5] = new TPiece(host);
        previewPieces[6] = new ZPiece(host);

        // starting pieces
        activeIndex = gameStateProxy.activeIndex;
        previewIndex = gameStateProxy.previewIndex;
        activePieces[activeIndex].setActive(true);

        //paused = true;
        //restartMe = false;
        stateOfTheGame = gameStateProxy.stateOfTheGame;
        scheduleSpawn = gameStateProxy.scheduleSpawn;
        spawnTime = gameStateProxy.spawnTime;
    }

    public void setPlayerName(String string) {
        playerName = string;
    }

    public Board getBoard() {
        return board;
    }

	public String getPlayerName() {
		return playerName;
	}
	
	public int getAutoDropInterval() {
		return dropIntervals[Math.min(level,maxLevel)];
	}
	
	public long getMoveInterval() {
		return playerMoveInterval;
	}
	
	public long getSoftDropInterval() {
		return playerDropInterval;
	}
	
	public void setRunning(boolean b) {
		if(b) {
			currentTime = System.currentTimeMillis();
			if(stateOfTheGame != state_finished)
				stateOfTheGame = state_running;
		} else {
			if(stateOfTheGame == state_running)
				stateOfTheGame = state_paused;
		}
	}
	
	public void clearLines(boolean playerHardDrop, int hardDropDistance) {
		if(host == null)
			return;

		activePieces[activeIndex].place(board);
		int cleared = board.clearLines(activePieces[activeIndex].getDim());
		clearedLines += cleared;
		long addScore;
		
		switch(cleared) {
			case 1:
				addScore = singleLineScore;
				multitetris = false;
				host.sound.clearSound();
				popupTime = gameTime;
				break;
			case 2:
				addScore = doubleLineScore;
				multitetris = false;
				host.sound.clearSound();
				popupTime = gameTime;
				break;
			case 3:
				addScore = trippleLineScore;
				multitetris = false;
				host.sound.clearSound();
				popupTime = gameTime;
				break;
			case 4:
				if(multitetris)
					addScore = multiTetrisScore;
				else
					addScore = quadLineScore;
				multitetris = true;
				host.sound.tetrisSound();
				popupTime = gameTime;
				break;
			default:
				addScore = 0;
				//consecutiveBonusScore = 0;
				host.sound.dropSound();
				if((gameTime - popupTime) < (popupAttack + popupSustain))
					popupTime = gameTime - (popupAttack + popupSustain);
				break;
		}
		//long tempBonus = consecutiveBonusScore;
		//consecutiveBonusScore += addScore;
		if(cleared > 0) {
			/* HardDrop/SoftDrop Boni: we comply to Tetrisfriends rules now */
			if(playerHardDrop) {
				addScore += hardDropDistance*hardDropBonus;
				//addScore = (int)((float)addScore* (1.0f + ((float)hardDropDistance/(float)hardDropBonusFactor)));
			} else {
				addScore += softDropDistance*softDropBonus;
			}
		}
		score += addScore;// + tempBonus;
		if(addScore != 0)
			popupString = "+"+addScore;
		// host.saveScore(score); is not supported by ScoreDataSource
	}

	public void pieceTransition(boolean eventVibrationEnabled) {
		if(host == null)
			return;
		
		scheduleSpawn = true;
		//Delay Piece Transition only while vibration is playing
		if(eventVibrationEnabled)
			spawnTime = gameTime + spawn_delay;
		else
			spawnTime = gameTime;
		
		activePieces[activeIndex].reset(host);
		activeIndex  = previewIndex;
		previewIndex = rng.next();
		activePieces[activeIndex].reset(host);
	}
	
	public void hold() {
		if(host == null)
			return;
		
		
	}
	
	public void finishTransition() {
		if(host == null)
			return;
		
		scheduleSpawn = false;
		host.display.invalidatePhantom();
		activePieces[activeIndex].setActive(true);
		setNextDropTime(gameTime + dropIntervals[Math.min(level,maxLevel)]);
		setNextPlayerDropTime(gameTime);
		setNextPlayerMoveTime(gameTime);
		softDropDistance = 0;
		
		// Checking for Defeat
		if(!activePieces[activeIndex].setPosition(piece_start_x, 0, false, board)) {
			stateOfTheGame = state_finished;
			host.sound.gameOverSound();
			host.gameOver(score, getTimeString(), (int)((float)actions*(60000.0f / gameTime)));
		}
	}
	
	public boolean isResumable() {
		return (stateOfTheGame != state_finished);
	}

	public String getScoreString() {
		return "" + score;
	}

	public Piece getActivePiece() {
		return activePieces[activeIndex];
	}

	/**
	 * 
	 * @param tempTime
	 * @return true if controls is allowed to cycle()
	 */
	public boolean cycle(long tempTime) {
		if(stateOfTheGame != state_running)
			return false;
		
		gameTime += (tempTime - currentTime);
		currentTime = tempTime;
		
		// Instant Placement
		if(scheduleSpawn) {
			if(gameTime >= spawnTime)
				finishTransition();
			return false;
		}
		return true;
	}

    public String getLevelString() {
        return infinity ? "-1" : "" + level;
    }

	public String getTimeString() {
		date.setTimeInMillis(gameTime + hourOffset*(3600000));
		return formatter.format(date.getTime());
	}

	public String getAPMString() {
		if(host == null)
			return "";
		return String.valueOf((int)((float)actions*(60000.0f / gameTime)));
	}
	
	@Override
	public void reconnect(GameActivity ga) {
		super.reconnect(ga);
		
		playerDropInterval = (int)(1000.0f / PreferenceManager.getDefaultSharedPreferences(ga).getInt("pref_softdropspeed", 60));
		playerMoveInterval = (int)(1000.0f / PreferenceManager.getDefaultSharedPreferences(ga).getInt("pref_movespeed", 60));
		
		if(PreferenceManager.getDefaultSharedPreferences(ga).getString("pref_rng", "sevenbag").equals("sevenbag") ||
				PreferenceManager.getDefaultSharedPreferences(ga).getString("pref_rng", "7-Bag-Randomization (default)").equals("7-Bag-Randomization (default)"))
			rng = new PieceGenerator(PieceGenerator.STRAT_7BAG);
		else
			rng = new PieceGenerator(PieceGenerator.STRAT_RANDOM);

		board.reconnect(ga);
		setRunning(true);
	}

	public void disconnect() {
		setRunning(false);
		board.disconnect();
		super.disconnect();
	}

	public Piece getPreviewPiece() {
		return previewPieces[previewIndex];
	}

	public long getTime() {
		return gameTime;
	}

    public void nextLevel() {
        if (!infinity)
            level++;
    }

	public int getLevel() {
		return level;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public int getClearedLines() {
		return clearedLines;
	}

	public void action() {
		actions++;
	}

	public void setNextPlayerDropTime(long time) {
		nextPlayerDropTime = time;
	}

	public void setNextPlayerMoveTime(long time) {
		nextPlayerMoveTime = time;
	}

	public void setNextDropTime(long l) {
		nextDropTime = l;
	}

	public long getNextPlayerDropTime() {
		return nextPlayerDropTime;
	}

	public long getNextDropTime() {
		return nextDropTime;
	}

	public long getNextPlayerMoveTime() {
		return nextPlayerMoveTime;
	}

	public static void destroy() {
		if(instance != null)
			instance.disconnect();
		instance = null;
	}

	public static GameState getInstance(GameActivity ga) {
		if(instance == null)
			instance = new GameState(ga);
		return instance;
	}

    public static GameState getNewInstance(GameActivity ga, GameStateProxy gameStateProxy) {
        if(gameStateProxy == null){
            instance = new GameState(ga);
        } else  {
            instance = new GameState(ga, gameStateProxy);
        }

        return instance;
    }

	public static boolean hasInstance() {
		return (instance != null);
	}

	public long getScore() {
		return score;
	}

	public int getAPM() {
		return (int)((float)actions*(60000.0f / gameTime));
	}

	public int getSongtime() {
		return songtime;
	}
	
	public static boolean isFinished() {
		if(instance == null)
			return true;
		return !instance.isResumable();
	}

	public void setSongtime(int songtime) {
		this.songtime = songtime;
	}

    public void setLevel(int int1) {
        if (int1 < 0) {
            infinity = true;
            int1 = 0;
        }

        level = int1;
        nextDropTime = host.getResources().getIntArray(R.array.intervals)[int1];
        clearedLines = 10 * int1;
    }

	public String getPopupString() {
		return popupString;
	}

	public int getPopupAlpha() {
		long x = gameTime - popupTime;
		
		if(x < (popupAttack+popupSustain))
			return 255;
		
		if(x < (popupAttack+popupSustain+popupDecay))
			return (int)(255.0f*(1.0f + (((float)(popupAttack + popupSustain - x))/((float)popupDecay))));
		
		return 0;
	}

	public float getPopupSize() {
		long x = gameTime - popupTime;
		
		if(x < popupAttack)
			return (int)(60.0f*(1.0f + (((float)x)/((float)popupAttack))));
		
		return 120;
	}

	public int getPopupColor() {
		if(host == null)
			return 0;
		
		if(multitetris)
			return host.getResources().getColor(R.color.yellow);
		return host.getResources().getColor(color.white);
	}

    public void incSoftDropCounter() {
        softDropDistance++;
    }

    private Object writeReplace () {
//        Log.i("GameState","writeReplace()");
        return new GameStateProxy(this);
    }


    public static class GameStateProxy implements Serializable {
        private static final long serialVersionUID = 201807301756L;

        // References
        public  int[][] board;
        public int hourOffset;

        // Game State
        private String playerName;
        private int activeIndex, previewIndex;
        private boolean scheduleSpawn;
        private long spawnTime;
        private int stateOfTheGame;
        private long score;
        private int clearedLines;
        private int level;
        private int maxLevel;
        private long gameTime;     // += (systemtime - currenttime) at start of cycle
        private long currentTime;  // = systemtime at start of cycle
        private long nextDropTime;
        private long nextPlayerDropTime;
        private long nextPlayerMoveTime;
        private int[] dropIntervals; // =(1/gamespeed)
        private long playerDropInterval;
        private long playerMoveInterval;
        private int singleLineScore;
        private int doubleLineScore;
        private int trippleLineScore;
        private int multiTetrisScore;
        private boolean multitetris;
        private int quadLineScore;
        private int hardDropBonus;
        private int softDropBonus;
        private int spawn_delay;
        private int piece_start_x;
        private long actions;
        private int songtime;

        private long popupTime;
        private String popupString;
        private int popupAttack;
        private int popupSustain;
        private int popupDecay;
        private int softDropDistance;

        private boolean infinity = false;

        public GameStateProxy(GameState gameState) {
            int width = gameState.board.getWidth();
            int height = gameState.board.getHeight();
            board = new int[height][width];
            for(int i = 0; i < height; i++){
                for(int j = 0; j < width; j++){
                    Square square = gameState.board.get(i,j);
                    board[i][j] = square == null? -1 : square.getType();
                }
            }

            this.hourOffset = gameState.hourOffset;
            this.playerName = gameState.playerName;
            this.activeIndex = gameState.activeIndex;
            this.previewIndex = gameState.previewIndex;
            this.scheduleSpawn = gameState.scheduleSpawn;
            this.stateOfTheGame = gameState.stateOfTheGame;
            this.score = gameState.score;
            this.clearedLines = gameState.clearedLines;
            this.level = gameState.level;
            this.maxLevel = gameState.maxLevel;
            this.gameTime = gameState.gameTime;
            this.currentTime = gameState.currentTime;
            this.nextDropTime = gameState.nextDropTime;
            this.nextPlayerDropTime = gameState.nextPlayerDropTime;
            this.nextPlayerMoveTime = gameState.nextPlayerMoveTime;
            this.dropIntervals = gameState.dropIntervals;
            this.playerDropInterval = gameState.playerDropInterval;
            this.playerMoveInterval = gameState.playerMoveInterval;
            this.singleLineScore = gameState.singleLineScore;
            this.doubleLineScore = gameState.doubleLineScore;
            this.trippleLineScore = gameState.trippleLineScore;
            this.multiTetrisScore = gameState.multiTetrisScore;
            this.multitetris = gameState.multitetris;
            this.quadLineScore = gameState.quadLineScore;
            this.hardDropBonus = gameState.hardDropBonus;
            this.softDropBonus = gameState.softDropBonus;
            this.spawn_delay = gameState.spawn_delay;
            this.piece_start_x = gameState.piece_start_x;
            this.actions = gameState.actions;
            this.songtime = gameState.songtime;

            this.popupTime = gameState.popupTime;
            this.popupString = gameState.popupString;
            this.popupAttack = gameState.popupAttack;
            this.popupSustain = gameState.popupSustain;
            this.popupDecay = gameState.popupDecay;
            this.softDropDistance = gameState.softDropDistance;
            this.infinity  = gameState.infinity;

        }

        public boolean isResumable() {
            return (stateOfTheGame != state_finished);
        }
    }



}