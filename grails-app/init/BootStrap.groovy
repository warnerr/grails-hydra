import com.seven10.hydra.user.Role
import com.seven10.hydra.user.User
import com.seven10.hydra.user.UserRole

class BootStrap {

    def init = { servletContext ->

        def adminRole = new Role('ROLE_ADMIN').save()
        def userRole = new Role('ROLE_USER').save()
        def apiRole = new Role('ROLE_API_KEY').save()

        def testUser = new User('me', 'password').save()

        UserRole.create testUser, adminRole

        UserRole.withSession {
            it.flush()
            it.clear()
        }

        assert User.count() == 1
        assert Role.count() == 3
        assert UserRole.count() == 1
    }
    def destroy = {
    }
}
