module Config
  module SessionHelper
    def sign_in
      visit "/"

      expect(page).to have_content "You are not signed in!"

      set_cookie
    end

    def session_cookie_value
      MadekOpenSession.build_session_value(user) if user
    end

    def set_cookie
      Capybara.current_session.driver.browser.manage.add_cookie(
        name: "madek-session",
        value: session_cookie_value
      )
    end
  end
end
