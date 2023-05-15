package world.waldens;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
// Import everything in the CommandManager
import static net.minecraft.server.command.CommandManager.*;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.ScoreboardCriterion.RenderType;

public final class WWScoreboardCommand {

    private static final SimpleCommandExceptionType INVALID_EXCEPTION = new SimpleCommandExceptionType(Text.literal("The WALDENS WORLD SCOREBOARD BY WALDEN (TM) input was invalid"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        dispatcher.register(
            literal("wwscoreboard")
                .requires(source -> source.hasPermissionLevel(2))
                .then(
                    argument("scores", StringArgumentType.string())
                        .executes(ctx -> execute(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "scores"),
                            "  Walden's World  "
                        ))
                        .then(
                            argument("title", StringArgumentType.string())
                            .executes(ctx -> execute(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "scores"),
                                StringArgumentType.getString(ctx, "title")
                            ))
                        )
                )
        );

    }

    private static int execute(ServerCommandSource source, String text, String title) throws CommandSyntaxException
    {
        String[] input = text.split(",");

        try {
            for (int i = 0; i < input.length; i++) {
                String[] split = input[i].trim().split(" ");
                if (split.length < 2) {
                    throw INVALID_EXCEPTION.create();
                }
                // attempt to parse integer, if fail it will throw exception
                Integer.parseInt(split[split.length - 1]);
            }
        }
        catch (Exception e) {
            throw INVALID_EXCEPTION.create();
        }


        Scoreboard scoreboard = source.getServer().getScoreboard();

        ScoreboardObjective objective = scoreboard.getObjective("wwscoreboard");

        if (objective != null) {
            scoreboard.removeObjective(objective);
        }

        objective = scoreboard.addObjective("wwscoreboard", ScoreboardCriterion.DUMMY, Text.of(title), RenderType.INTEGER);

        for (int i = 0; i < input.length; i++) {
            String[] split = input[i].trim().split(" ");

            String name = "";
            for (int x = 0; x < split.length - 1; x++) {
                name += split[x] + " ";
            }
            name = name.trim();

            int score = Integer.parseInt(split[split.length - 1]);

            addPlayerWithScore(scoreboard, objective, name, name, score);
        }

        // Display the scoreboard
        scoreboard.setObjectiveSlot(1, objective);

        return 1;
    }

    private static void addPlayerWithScore(Scoreboard scoreboard, ScoreboardObjective objective, String playerName, String displayName, int score) {
        Team team = scoreboard.getTeam(playerName);
        if (team == null) {
            team = scoreboard.addTeam(playerName);
        }
        team.setDisplayName(Text.of(displayName));

        ScoreboardPlayerScore playerScore = scoreboard.getPlayerScore(playerName, objective);
        playerScore.setScore(score);
    }
  
}
