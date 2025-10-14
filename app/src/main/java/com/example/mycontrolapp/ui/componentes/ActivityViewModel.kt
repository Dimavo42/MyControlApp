package com.example.mycontrolapp.ui.componentes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycontrolapp.logic.Activity
import com.example.mycontrolapp.logic.ActivityRoleRequirement
import com.example.mycontrolapp.logic.Assignment
import com.example.mycontrolapp.logic.ListManager
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedEnums.Profession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val listManager: ListManager
) : ViewModel() {

    private val sharing = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)

    /* ---------------------------- UI filters ---------------------------- */

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()
    fun setSelectedYearMonth(ym: YearMonth) { _selectedYearMonth.value = ym }

    private val _selectedUserId = MutableStateFlow<String?>(null)
    val selectedUserId: StateFlow<String?> = _selectedUserId.asStateFlow()
    fun setSelectedUser(userId: String?) { _selectedUserId.value = userId }

    /* ---------------------------- Source flows ---------------------------- */

    val activitiesFlow: StateFlow<List<Activity>> =
        listManager.activitiesFlow()
            .stateIn(viewModelScope, sharing, emptyList())

    val usersFlow: StateFlow<List<User>> =
        listManager.usersFlow()
            .stateIn(viewModelScope, sharing, emptyList())

    val assignmentsFlow: StateFlow<List<Assignment>> =
        listManager.assignmentsFlow()
            .stateIn(viewModelScope, sharing, emptyList())

    fun userProfessionsFlow(userId: String) =
        listManager.userProfessionsFlow(userId).distinctUntilChanged()

    /* --------------------- Activities with assigned users ------------------ */

    data class ActivityWithUsers(val activity: Activity, val users: List<User>)

    val activitiesWithUsersFlow: StateFlow<List<ActivityWithUsers>> =
        combine(activitiesFlow, usersFlow, assignmentsFlow) { activities, users, assignments ->
            val usersById = users.associateBy { it.id }
            val userIdsByActivity = assignments.groupBy { it.activityId }
                .mapValues { (_, asg) -> asg.map { it.userId } }

            activities.map { act ->
                val assignedUsers = userIdsByActivity[act.id].orEmpty().mapNotNull { usersById[it] }
                ActivityWithUsers(act, assignedUsers)
            }
        }.stateIn(viewModelScope, sharing, emptyList())

    val activitiesWithUsersFilteredFlow: StateFlow<List<ActivityWithUsers>> =
        combine(activitiesWithUsersFlow, selectedYearMonth, selectedUserId) { list, ym, userId ->
            list.asSequence()
                .filter { awu ->
                    val inMonth = awu.activity.inYearMonth(ym)
                    val matchesUser = userId == null || awu.users.any { it.id == userId }
                    inMonth && matchesUser
                }
                .sortedBy { it.activity.startAt }
                .toList()
        }.stateIn(viewModelScope, sharing, emptyList())

    val activitiesByDateFilteredFlow: StateFlow<Map<LocalDate, List<Activity>>> =
        activitiesWithUsersFilteredFlow
            .map { awuList ->
                awuList.map { it.activity }
                    .groupBy { act -> LocalDate.ofEpochDay(act.dateEpochDay.toLong()) }
            }
            .stateIn(viewModelScope, sharing, emptyMap())

    /* --------------------- Aggregated counters (one flow) ------------------ */

    // Small DTOs your repository returns (see changes below).
    data class Counters(val assigned: Int, val required: Int)

    /** Map: activityId -> Counters(assigned, required). */
    val activityCountersFlow: StateFlow<Map<String, Counters>> =
        combine(
            listManager.assignedCountsAllFlow().distinctUntilChanged(),
            listManager.requiredCountsAllFlow().distinctUntilChanged()
        ) { assignedRows, requiredRows ->
            val a = assignedRows.associate { it.activityId to it.assigned }
            val r = requiredRows.associate { it.activityId to it.required }
            (a.keys + r.keys).associateWith { id ->
                Counters(assigned = a[id] ?: 0, required = r[id] ?: 0)
            }
        }.stateIn(viewModelScope, sharing, emptyMap())

    /** Convenience if you want a Flow per activity *from the map* (still cheap). */
    fun countersFor(activityId: String): Flow<Counters> =
        activityCountersFlow
            .map { it[activityId] ?: Counters(0, 0) }
            .distinctUntilChanged()

    /* ------------------------------- Mutations ----------------------------- */

    fun insertActivity(a: Activity) = viewModelScope.launch { listManager.addActivity(a) }
    fun updateActivity(a: Activity) = viewModelScope.launch { listManager.updateActivity(a) }
    fun removeActivity(id: String)   = viewModelScope.launch { listManager.removeActivity(id) }

    fun insertUser(u: User) = viewModelScope.launch { listManager.addUser(u) }
    fun updateUser(u: User) = viewModelScope.launch { listManager.updateUser(u) }
    fun removeUser(id: String) = viewModelScope.launch { listManager.removeUser(id) }

    fun insertActivityWithRequirements(
        activity: Activity,
        roles: List<Profession>
    ) = viewModelScope.launch {
        listManager.addActivity(activity)

        val counts: Map<Profession, Int> =
            roles.filter { it != Profession.Unknown }
                .groupingBy { it }
                .eachCount()

        val reqs = counts.map { (prof, cnt) ->
            ActivityRoleRequirement(
                activityId = activity.id,
                profession = prof,
                requiredCount = cnt
            )
        }
        listManager.deleteAllRequirementsForActivity(activity.id)
        if (reqs.isNotEmpty()) {
            listManager.upsertAllRequirements(reqs)
        }
    }

    fun assignUser(activityId: String, userId: String, profession: Profession) = viewModelScope.launch {
        listManager.addAssignment(Assignment(activityId = activityId, userId = userId, role = profession))
    }

    fun unassignUser(activityId: String, userId: String) = viewModelScope.launch {
        assignmentsFlow.value
            .firstOrNull { it.activityId == activityId && it.userId == userId }
            ?.let { listManager.removeAssignment(it.id) }
    }

    fun insertRequirement(a: ActivityRoleRequirement) =
        viewModelScope.launch { listManager.upsertRequirement(a) }

    fun replaceUserProfessions(userId: String, professions: Set<Profession>){

        viewModelScope.launch { listManager.replaceUserProfessions(userId, professions) }
    }

    /* ----------------------------- Role requirements & helpers ----------------------------- */

    fun roleRequirementsFlow(activityId: String): Flow<List<ActivityRoleRequirement>> =
        listManager.roleRequirementsFlow(activityId)

    fun usersNotAssignedToActivity(activityId: String): Flow<List<User>> =
        combine(usersFlow, assignmentsFlow) { users, assignments ->
            val assignedIds = assignments.asSequence()
                .filter { it.activityId == activityId }
                .map { it.userId }
                .toSet()
            users.filter { it.id !in assignedIds }
        }


    // Legacy helpers (you can keep them if other screens use them, but
    // DO NOT collect them per-row in lists anymore).
    fun requiredCountForActivity(activityId: String) =
        roleRequirementsFlow(activityId).map { reqs -> reqs.sumOf { it.requiredCount } }

    fun assignedCountForActivityFlow(activityId: String): Flow<Int> =
        listManager.assignedCountForActivityFlow(activityId)

    /* ----------------------------- Helpers -------------------------------- */

    private fun Activity.inYearMonth(ym: YearMonth): Boolean {
        val d = LocalDate.ofEpochDay(this.dateEpochDay.toLong())
        return d.year == ym.year && d.month == ym.month
    }
}

