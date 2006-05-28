/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.sound;

import games.stendhal.client.StendhalClient;
import games.stendhal.client.entity.Entity;
import games.stendhal.client.entity.Player;
import games.stendhal.client.entity.SoundObject;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import org.apache.log4j.Logger;

import marauroa.common.Log4J;
import marauroa.common.game.RPObject;

/**
 * An ambient sound is a compound sound consisting of any number of loop
 * sounds and cycle sounds. Loop sounds play continuously without
 * interruption, cycle sounds work as described in class SoundCycle. The
 * ambient sound can be played global or fixed to a map location.
 * 
 * @author Jane Hunt
 */
public class AmbientSound
{
  /** the logger instance. */
  private static final Logger         logger             = Log4J.getLogger(AmbientSound.class);

  private List<LoopSoundInfo> loopSounds = new ArrayList<LoopSoundInfo>();

  private List<SoundCycle>    cycleList  = new ArrayList<SoundCycle>();

  private String              name;

  private SoundObject         soundObject;

  private Point2D             soundPos;

  private Point2D             playerPos;

  private Rectangle2D         playerHearing;

  private float               loudnessDB;

  private boolean             playing;

  /**
   * The LoopSoundInfo stores information which is required to start the
   * continuously looping sound elements of this ambient sound.
   */
  private static class LoopSoundInfo implements Cloneable
  {
    String name;

    float  loudnessDB;

    int    delay;

    Clip   clip;

    public LoopSoundInfo(String sound, int volume, int delay)
    {
      name = sound;
      loudnessDB = SoundSystem.dBValues[volume];
      this.delay = delay;
    }

    /**
     * Returns a copy of this LoopSoundInfo with <code>clip</code> set to
     * <b>null</b> (clip not playing).
     */
    public LoopSoundInfo clone()
    {
      LoopSoundInfo si;

      try
      {
        si = (LoopSoundInfo) super.clone();
        si.clip = null;
      } catch (CloneNotSupportedException e)
      {
        System.out.println("#### bad clone");
        return null;
      }

      return si;
    }

    public synchronized void stopClip()
    {
      if (clip != null)
      {
        clip.stop();
        clip = null;
      }
    }
  }

  private class SoundStarter extends Thread
  {
    LoopSoundInfo soundInfo;

    float         correctionDB;

    /** Starts a looping sound. */
    public SoundStarter(LoopSoundInfo loopInfo, float correctionDB)
    {
      this.soundInfo = loopInfo;
      this.correctionDB = correctionDB;
    }

    public void run()
    {
      ClipRunner libClip;

      // get the library sound clip
      if ((libClip = SoundSystem.get().getSoundClip(soundInfo.name)) == null)
        throw new IllegalArgumentException("sound unknown: " + soundInfo.name);

      // handle delay phase request on sample start
      if (soundInfo.delay > 0)
        try
        {
          sleep(soundInfo.delay);
        } catch (InterruptedException e)
        {
        }

      synchronized (soundInfo)
      {
        // terminate an existing sound
        soundInfo.stopClip();

        // start playing
        soundInfo.clip = libClip.getAudioClip(SoundSystem.get().getVolume(), loudnessDB + soundInfo.loudnessDB + correctionDB);
        if (soundInfo.clip != null)
        {
          soundInfo.clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        playing = true;
      }
    }
  } // SoundStarter

  /**
   * Creates an unlocalized ambient sound (plays everywhere) with the given
   * overall volume setting.
   * 
   * @param volume
   *          int 0..100 loudness of ambient sound in total
   */
  public AmbientSound(String name, int volume)
  {
    this(name, null, 0, volume);
  }

  /**
   * Creates a map-localized ambient sound with the given overall volume
   * setting.
   * 
   * @param name
   *          ambient sound name
   * @param point
   *          <code>Point2D</code> map location expressed in coordinate
   *          units
   * @param radius
   *          audibility radius of sound object in coordinate units
   * @param volume
   *          int 0..100 loudness of ambient sound in total
   */
  public AmbientSound(String name, Point2D point, int radius, int volume)
  {
    String hstr;

    if (name == null)
      throw new NullPointerException();

    if (radius < 0 | volume < 0 | volume > 100)
      throw new IllegalArgumentException("r=" + radius + ", v=" + volume);

    this.name = name;
    soundPos = point;
    loudnessDB = SoundSystem.dBValues[volume];

    if (soundPos != null)
    {
      soundObject = new SoundObject();
      soundObject.setLocation(soundPos);
      soundObject.setAudibleRange(radius);
    }

    if (soundObject != null)
      hstr = "-- created LOC AMBIENT: " + name + " at (" + (int) soundObject.getx() + "," + (int) soundObject.gety() + "), rad=" + radius + " vol=" + volume;
    else
      hstr = "-- created GLOB AMBIENT: " + name + ", vol=" + volume;

    logger.debug(hstr);
  } // constructor

  /**
   * Creates a map-localized ambient sound with the given settings and the
   * sound composition taken from the parameter ambient sound. (The paradigm
   * presets content equivalent to <code>addCycle()</code> and
   * <code>addLoop()</code> calls.)
   * 
   * @param sound
   *          <code>AmbientSound</code> as paradigm for sound composition
   * @param name
   *          ambient sound name
   * @param point
   *          <code>Point2D</code> map location expressed in coordinate
   *          units
   * @param radius
   *          audibility radius of sound object in coordinate units
   * @param volume
   *          int 0..100 loudness of ambient sound in total
   */
  public AmbientSound(AmbientSound sound, String name, Point2D point, int radius, int volume)
  {
    this(name, point, radius, volume);

    for (LoopSoundInfo c : sound.loopSounds)
      loopSounds.add(c.clone());

    for (SoundCycle c : sound.cycleList)
    {
      SoundCycle cycle = c.clone();
      if (soundObject != null)
        cycle.entityRef = new WeakReference<Entity>(soundObject);
      else
        cycle.entityRef = null;
      cycleList.add(cycle);
    }

    String hstr = "-- content supplied to " + name + ": " + loopSounds.size() + " loops, " + cycleList.size() + " cycles";
    logger.debug(hstr);
  } // constructor

  /**
   * This adds a loop sound to the ambient sound definition.
   * 
   * @param sound
   *          library sound name
   * @param volume
   *          relative play volume of the added sound
   * @param delay
   *          milliseconds of start delay for playing the sound
   */
  public void addLoop(String sound, int volume, int delay)
  {
    SoundSystem sys;
    LoopSoundInfo info;

    sys = SoundSystem.get();
    if (!sys.contains(sound))
    {
      logger.error("*** Ambient Sound: missing sound definition (" + sound + ")");
      return;
    }

    info = new LoopSoundInfo(sound, volume, delay);
    loopSounds.add(info);
  } // addLoop

  public void addCycle(String token, int period, int volBot, int volTop, int chance)
  {
    SoundCycle cycle;

    cycle = new SoundCycle(soundObject, token, period, volBot, volTop, chance);
    cycleList.add(cycle);
  } // addCycle

  private boolean canPlay()
  {
    return soundPos == null || (playerHearing.contains(soundPos) && soundObject.getAudibleArea().contains(playerPos));
  }

  /**
   * Starts playing this ambient sound. This will take required actions, if
   * this ambient sound is not yet playing, to make it audible, global or
   * relative to the player's position depending on this sound's initializer.
   * This does nothing if this sound is already playing. Playing is suppressed
   * if sound position is outside the hearing range of the player.
   */
  protected void play()
  {
    play(getPlayer());
  }

  /**
   * Starts playing this ambient sound with the given player's hearing
   * parameters. This will take required actions, if this ambient sound is not
   * yet playing, to make it audible, global or relative to the player's
   * position depending on this sound's initializer. This does nothing if this
   * sound is already playing. Playing is suppressed if sound position is
   * outside the hearing range of the player.
   * 
   * @param player
   *          the client player object
   */
  protected void play(Player player)
  {
    float fogDB;

    if (playing)
      return;

    stop();

    // if map-localized
    if (soundPos != null)
    {
      // adjust to player settings
      if (player != null)
      {
        playerPos = player.getPosition();
        playerHearing = player.getHearingArea();

        // return if sound object is out of range
        if (!canPlay())
          return;
      }
      // return undone if no player
      else
        return;
    }

    // create and start loop sounds
    synchronized (loopSounds)
    {
      fogDB = getPlayerVolume();
      for (LoopSoundInfo info : loopSounds)
      {
        new SoundStarter(info, fogDB).start();
      }
    }

    // start cycle sounds
    for (SoundCycle c : cycleList)
    {
      c.play();
    }

    playing = true;
    String hstr = "- playing ambient: " + name;
    logger.debug(hstr);
    // System.out.println( hstr );
  } // play

  /** (Temporarily) stops playing this ambient sound. */
  protected void stop()
  {
    if (!playing)
      return;

    // terminate loop sounds
    synchronized (loopSounds)
    {
      for (LoopSoundInfo info : loopSounds)
      {
        info.stopClip();
      }
    }

    // stop cycle sounds
    for (SoundCycle c : cycleList)
    {
      c.stopPlaying();
    }

    playing = false;
    String hstr = "- stopped ambient: " + name;
    logger.debug(hstr);
    // System.out.println( hstr );
  } // stop

  /** Unrevokably terminates this ambient sound. */
  public void terminate()
  {
    stop();

    // terminate cycle sounds
    for (SoundCycle c : cycleList)
    {
      c.terminate();
    }

    // clear internal sound lists
    loopSounds.clear();
    cycleList.clear();

    // remove this object from sound system
    SoundSystem.stopAmbientSound(this);

    String hstr = "- terminated ambient: " + name;
    logger.debug(hstr);
    // System.out.println( hstr );
  } // terminate

  /**
   * Returns the sound volume for this ambient sound relative to the current
   * player position (fog correction value). Returns 0.0 if this sound is not
   * map-localized.
   * 
   * @return float dB correction of loudness
   */
  private float getPlayerVolume()
  {
    double distance, maxDist;
    int fogVolume;

    // if the sound is global (no position)
    if (soundPos == null)
      return 0;

    // if the sound is map localized
    else
    {
      // maximum fog if no player infos available
      if (playerPos == null | playerHearing == null)
      {
        // System.out.println( "ambient (" + name + ") fog volume: 0
        // (player unavailable)" );
        return SoundSystem.dBValues[0];
      }

      // determine sound volume cutoff due to distance (fog value)
      distance = soundPos.distance(playerPos);
      maxDist = playerHearing.getWidth() / 2;
      // System.out.println("ambient player hearing radius: " +
      // maxDist );
      fogVolume = Math.max(0, (int) (95 * (maxDist - distance) / maxDist + 5));
      // System.out.println( "ambient (" + name + ") fog volume:
      // dist=" + (int)distance + ", fog=" + fogVolume );
      return SoundSystem.dBValues[fogVolume];
    }
  } // getPlayerVolume

  /**
   * Informs this ambient sound about the actual player's position and hearing
   * parameters. Does nothing if player is <b>null</b> or the sound is not
   * map-localized. (Otherwise this will adjust sound fog loudness.)
   * 
   * @param player
   *          client player object (may be <b>null</b>
   */
  public void performPlayerMoved(Player player)
  {
    SoundSystem sys;

    // operation control
    sys = SoundSystem.get();
    if (!sys.isOperative() | sys.getMute() | player == null | soundPos == null)
      return;

    // if not yet playing, start playing
    if (!playing)
      play(player);

    // if playing, correct loudness/status of clips
    else
    {
      // set new player parameters
      playerPos = player.getPosition();
      playerHearing = player.getHearingArea();

      // decide on stopping to play (when sound object has moved out
      // of range)
      if (!canPlay())
      {
        stop();
      }
      // or updating sound loudness
      else
      {
        updateVolume();
      }
    }
  } // performPlayerPosition

  public void updateVolume()
  {
    FloatControl volCtrl;
    float fogDB;

    // detect player loudness fog value
    fogDB = getPlayerVolume();

    // set corrected volume to all running clips
    synchronized (loopSounds)
    {
      for (LoopSoundInfo info : loopSounds)
      {
        if (info.clip != null)
        {
          volCtrl = (FloatControl) info.clip.getControl(FloatControl.Type.MASTER_GAIN);
          volCtrl.setValue(SoundSystem.get().getVolumeDelta() + loudnessDB + info.loudnessDB + fogDB);
        }
      }
    }
  } // updateVolume

  /**
   * Returns the game Player object or <b>null</b> if unavailable or the
   * sound is not map-localized.
   * 
   * @return Player object
   */
  private Player getPlayer()
  {
    Player player = null;
    RPObject playerObj;

    // try to obtain player parameters if relative playing is ordered
    if (soundPos != null)
    {
      if ((playerObj = StendhalClient.get().getPlayer()) != null)
        player = (Player) StendhalClient.get().getGameObjects().get(playerObj.getID());
    }
    return player;
  }
}
