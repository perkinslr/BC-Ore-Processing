package net.ndrei.bcoreprocessing.lib.fluids

import buildcraft.api.fuels.BuildcraftFuelRegistry
import buildcraft.api.recipes.BuildcraftRecipeRegistry
import net.minecraft.util.JsonUtils
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.ndrei.bcoreprocessing.BCOreProcessing
import net.ndrei.bcoreprocessing.MOD_ID

@Suppress("unused")
object FluidsRegistry {
    lateinit var GASEOUS_LAVA: Array<BCFluidBase>

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    fun registerFluids() {
        GASEOUS_LAVA = registerFluid("gaseous_lava", 0x99FF0000.toInt(), 15, isGaseous = true).also {
            it.forEach {
                BuildcraftFuelRegistry.fuel?.addFuel(it, when (it.fluidTemperature) {
                    FluidTemperature.COOL -> 20000
                    else -> return@forEach
//                FluidTemperature.HOT -> 20000
//                FluidTemperature.SEARING -> 30000
                }.toLong(), 420)
            }
        }

        BCOreProcessing.configHelper.readExtraRecipesFile("fluids") {
            val name = JsonUtils.getString(it, "name", null) ?: return@readExtraRecipesFile
            val color = JsonUtils.getInt(it, "color", 0)
            val luminosity = JsonUtils.getInt(it, "luminosity", 0)
            val density = JsonUtils.getInt(it, "density", 1000)
            val viscosity = JsonUtils.getInt(it, "viscosity", 1000)
            val gaseous = JsonUtils.getBoolean(it, "gaseous", false)

            registerFluid(name, color, luminosity, density, viscosity, gaseous)
        }
    }

    @SubscribeEvent
    fun onTextureStitch(ev: TextureStitchEvent) {
        // TODO: find out why this is needed in order to correctly render this mod's fluids... :S
        ev.map.registerSprite(ResourceLocation(MOD_ID, "blocks/base_fluid_still"))
        ev.map.registerSprite(ResourceLocation(MOD_ID, "blocks/base_fluid_flow"))
    }

    private fun registerFluid(name: String, color: Int, luminosity: Int = 0, density: Int = 1000, viscosity: Int = 1000, isGaseous: Boolean = false): Array<BCFluidBase> {
        val still = ResourceLocation(MOD_ID, "blocks/base_fluid_still")
        val flowing = ResourceLocation(MOD_ID, "blocks/base_fluid_flow")

        return arrayOf(FluidTemperature.COOL, FluidTemperature.HOT, FluidTemperature.SEARING)
            .map {
                BCFluidBase(name, still, flowing, color, it, luminosity, density, viscosity, isGaseous)
                    .also {
                        FluidRegistry.registerFluid(it)
                        FluidRegistry.addBucketForFluid(it)
                    }
            }.toTypedArray().also {
            val registry = BuildcraftRecipeRegistry.refineryRecipes
            if (registry != null) {
                (1 until it.size).forEach { i ->
                    val cold = FluidStack(it[i - 1], 100)
                    val hot = FluidStack(it[i], 100)
                    registry.addCoolableRecipe(hot, cold, i, i - 1)
                    registry.addHeatableRecipe(cold, hot, i - 1, i)
                }
            }
        }
    }
}
