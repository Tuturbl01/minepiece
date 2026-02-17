package com.minepiecefarmer.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for caching and managing reflection-based access to Minecraft internals.
 * Provides robust fallback mechanisms and comprehensive logging.
 */
public class ReflectionCache {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MinepieceFarmer/Reflection");
    
    // Cached reflection members
    private static Method doAttackMethod = null;
    private static Field selectedSlotField = null;
    
    // Initialization flags
    private static boolean attackMethodInitialized = false;
    private static boolean slotFieldInitialized = false;
    
    /**
     * Initializes all reflection caches. Should be called during mod initialization.
     * This is optional - methods will auto-initialize on first use.
     */
    public static void initialize() {
        LOGGER.info("Initializing reflection cache...");
        try {
            findDoAttackMethod();
            findSelectedSlotField();
            LOGGER.info("Reflection cache initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Error during reflection cache initialization: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Attempts to invoke the doAttack method on the client.
     * Returns true if attack was successfully invoked, false otherwise.
     */
    public static boolean invokeDoAttack(MinecraftClient client) {
        if (client == null) {
            LOGGER.warn("Cannot invoke doAttack: client is null");
            return false;
        }
        
        if (!attackMethodInitialized) {
            findDoAttackMethod();
        }
        
        if (doAttackMethod == null) {
            LOGGER.debug("doAttack method not available");
            return false;
        }
        
        try {
            Object result = doAttackMethod.invoke(client);
            return result instanceof Boolean && (Boolean) result;
        } catch (IllegalAccessException e) {
            LOGGER.error("Access denied to doAttack method: {}", e.getMessage());
            doAttackMethod = null; // Invalidate cache
            attackMethodInitialized = false;
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error invoking doAttack: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Sets the selected hotbar slot for the player.
     * Returns true if successful, false otherwise.
     */
    public static boolean setSelectedSlot(PlayerInventory inventory, int slot) {
        if (inventory == null) {
            LOGGER.warn("Cannot set slot: inventory is null");
            return false;
        }
        
        if (slot < 0 || slot > 8) {
            LOGGER.warn("Invalid slot number: {}", slot);
            return false;
        }
        
        if (!slotFieldInitialized) {
            findSelectedSlotField();
        }
        
        if (selectedSlotField == null) {
            LOGGER.debug("selectedSlot field not available");
            return false;
        }
        
        try {
            selectedSlotField.setInt(inventory, slot);
            return true;
        } catch (IllegalAccessException e) {
            LOGGER.error("Access denied to selectedSlot field: {}", e.getMessage());
            selectedSlotField = null; // Invalidate cache
            slotFieldInitialized = false;
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error setting slot: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the currently selected hotbar slot.
     * Returns -1 if unable to retrieve.
     */
    public static int getSelectedSlot(PlayerInventory inventory) {
        if (inventory == null) {
            LOGGER.warn("Cannot get slot: inventory is null");
            return -1;
        }
        
        if (!slotFieldInitialized) {
            findSelectedSlotField();
        }
        
        if (selectedSlotField == null) {
            LOGGER.debug("selectedSlot field not available");
            return -1;
        }
        
        try {
            return selectedSlotField.getInt(inventory);
        } catch (Exception e) {
            LOGGER.warn("Error getting slot: {}", e.getMessage());
            return -1;
        }
    }
    
    // ═══════════ PRIVATE INITIALIZATION METHODS ═══════════
    
    /**
     * Finds and caches the doAttack method.
     * Uses multiple strategies for maximum compatibility.
     */
    private static synchronized void findDoAttackMethod() {
        if (attackMethodInitialized) return;
        attackMethodInitialized = true;
        
        LOGGER.debug("Searching for doAttack method...");
        
        try {
            // Strategy 1: Search by name patterns
            for (Method method : MinecraftClient.class.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) continue;
                if (method.getReturnType() != boolean.class) continue;
                
                String name = method.getName();
                if (name.equals("doAttack") || 
                    name.equals("method_1536") || // Obfuscated name
                    name.contains("attack") || 
                    name.contains("Attack")) {
                    
                    method.setAccessible(true);
                    doAttackMethod = method;
                    LOGGER.info("Found doAttack method: {} ({})", 
                        name, method.getDeclaringClass().getSimpleName());
                    return;
                }
            }
            
            // Strategy 2: Fallback - find any boolean no-arg method with "attack" in name
            LOGGER.warn("Standard doAttack search failed, trying fallback...");
            for (Method method : MinecraftClient.class.getDeclaredMethods()) {
                String name = method.getName().toLowerCase();
                if (method.getParameterCount() == 0 && 
                    method.getReturnType() == boolean.class &&
                    name.contains("attack")) {
                    
                    method.setAccessible(true);
                    doAttackMethod = method;
                    LOGGER.info("Found doAttack method (fallback): {}", method.getName());
                    return;
                }
            }
            
            LOGGER.error("Could not find doAttack method - attacks will not work!");
            
        } catch (SecurityException e) {
            LOGGER.error("Security exception while searching for doAttack: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error while searching for doAttack: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Finds and caches the selectedSlot field.
     * Uses multiple strategies for maximum compatibility.
     */
    private static synchronized void findSelectedSlotField() {
        if (slotFieldInitialized) return;
        slotFieldInitialized = true;
        
        LOGGER.debug("Searching for selectedSlot field...");
        
        try {
            // Strategy 1: Search by name patterns
            for (Field field : PlayerInventory.class.getDeclaredFields()) {
                if (field.getType() != int.class) continue;
                
                String name = field.getName();
                if (name.equals("selectedSlot") ||
                    name.equals("field_7545") || // Obfuscated name
                    name.contains("selected") ||
                    name.contains("Slot")) {
                    
                    field.setAccessible(true);
                    selectedSlotField = field;
                    LOGGER.info("Found selectedSlot field: {} ({})", 
                        name, field.getDeclaringClass().getSimpleName());
                    return;
                }
            }
            
            // Strategy 2: Fallback - find int field with value 0-8
            // This requires creating a temporary player inventory which we can't do reliably
            // So we just try the first int field
            LOGGER.warn("Standard selectedSlot search failed, trying fallback...");
            for (Field field : PlayerInventory.class.getDeclaredFields()) {
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    selectedSlotField = field;
                    LOGGER.info("Found selectedSlot field (fallback): {}", field.getName());
                    return;
                }
            }
            
            LOGGER.error("Could not find selectedSlot field - slot switching will not work!");
            
        } catch (SecurityException e) {
            LOGGER.error("Security exception while searching for selectedSlot: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error while searching for selectedSlot: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clears the reflection cache. Useful for debugging or after mod reload.
     */
    public static void clearCache() {
        LOGGER.info("Clearing reflection cache");
        doAttackMethod = null;
        selectedSlotField = null;
        attackMethodInitialized = false;
        slotFieldInitialized = false;
    }
}
